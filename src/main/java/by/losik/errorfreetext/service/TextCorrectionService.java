package by.losik.errorfreetext.service;

import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.external.yandex.client.YandexSpellerClient;
import by.losik.errorfreetext.external.yandex.exception.YandexSpellerException;
import by.losik.errorfreetext.external.yandex.model.SpellResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextCorrectionService {

    private final YandexSpellerClient spellerClient;

    @Value("${app.correction.max-chunk-size:10000}")
    private int MAX_CHUNK_SIZE;

    @Value("${app.correction.ignore-urls:4}")
    private int ignoreUrls;

    @Value("${app.correction.ignore-digits:2}")
    private int ignoreDigits;

    public String correctText(String text, Language language) {
        log.info("Starting text correction for language: {}, text length: {}", language, text.length());

        List<String> chunks = splitText(text, MAX_CHUNK_SIZE);
        log.debug("Split text into {} chunks", chunks.size());

        int options = calculateOptions(text);
        log.debug("Using options: {}", options);

        List<List<SpellResult>> allResults = spellerClient.checkTextsWithRetry(
                chunks,
                language.name().toLowerCase(),
                options
        );

        if (allResults == null) {
            throw new YandexSpellerException("Yandex Speller API returned null response");
        }

        String correctedText = applyCorrections(text, chunks, allResults);
        log.info("Text correction completed. Original length: {}, corrected length: {}",
                text.length(), correctedText.length());

        return correctedText;
    }

    private List<String> splitText(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= maxChunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChunkSize, text.length());

            if (end < text.length()) {
                end = findBreakPoint(text, start, end);
            }

            chunks.add(text.substring(start, end));
            start = end;
        }

        return chunks;
    }

    private int findBreakPoint(String text, int start, int end) {

        int hardLimit = start + MAX_CHUNK_SIZE;

        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
            if (c == ',' || c == ';' || c == ':' || Character.isWhitespace(c)) {
                if (i + 1 <= hardLimit) {
                    return i + 1;
                }
            }
        }

        return hardLimit;
    }

    private int calculateOptions(String text) {
        int options = 0;
        if (containsDigits(text)) {
            options |= ignoreDigits;
        }
        if (containsUrls(text)) {
            options |= ignoreUrls;
        }
        return options;
    }

    private boolean containsDigits(String text) {
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUrls(String text) {
        UrlValidator validator = new UrlValidator(
                new String[]{"http", "https", "ftp", "file"},
                UrlValidator.ALLOW_LOCAL_URLS
        );

        for (String word : text.split("[\\s,;:\"'()\\[\\]{}<>]+")) {
            if (validator.isValid(word) ||
                    (word.startsWith("www.") && validator.isValid("http://" + word))) {
                return true;
            }
        }
        return false;
    }

    private String applyCorrections(String originalText, List<String> chunks,
                                    List<List<SpellResult>> allResults) {
        StringBuilder result = new StringBuilder(originalText);

        int globalOffset = 0;

        for (int chunkIndex = 0; chunkIndex < Math.min(chunks.size(), allResults.size()); chunkIndex++) {
            String chunk = chunks.get(chunkIndex);
            List<SpellResult> errors = allResults.get(chunkIndex);

            if (errors != null && !errors.isEmpty()) {
                errors.sort((e1, e2) -> Integer.compare(e2.getPos(), e1.getPos()));

                for (SpellResult error : errors) {
                    if (error.getSuggestions() != null && !error.getSuggestions().isEmpty()) {
                        String wrongWord = error.getWord();
                        String correctWord = error.getSuggestions().get(0);

                        if (!correctWord.equalsIgnoreCase(wrongWord)) {
                            int absolutePos = globalOffset + error.getPos();
                            int length = error.getLen();

                            if (absolutePos + length <= result.length() &&
                                    result.substring(absolutePos, absolutePos + length).equals(wrongWord)) {

                                result.replace(absolutePos, absolutePos + length, correctWord);

                                int diff = correctWord.length() - length;
                                if (diff != 0) {
                                    for (SpellResult remainingError : errors) {
                                        if (remainingError.getPos() > error.getPos()) {
                                            remainingError.setPos(remainingError.getPos() + diff);
                                        }
                                    }
                                }

                                log.debug("Replaced '{}' with '{}' at position {}",
                                        wrongWord, correctWord, absolutePos);
                            }
                        }
                    }
                }
            }

            globalOffset += chunk.length();
        }

        return result.toString();
    }
}