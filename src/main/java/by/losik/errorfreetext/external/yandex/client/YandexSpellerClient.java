package by.losik.errorfreetext.external.yandex.client;

import by.losik.errorfreetext.config.YandexSpellerConfig;
import by.losik.errorfreetext.external.yandex.exception.YandexSpellerException;
import by.losik.errorfreetext.external.yandex.model.SpellResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YandexSpellerClient {

    private final RestTemplate restTemplate;
    private final YandexSpellerConfig config;

    @CircuitBreaker(name = "yandexSpeller")
    @Retry(name = "yandexSpeller")
    @RateLimiter(name = "yandexSpeller")
    public List<List<SpellResult>> checkTextsWithRetry(
            List<String> texts, String lang, int options) {

        URI uri = buildUri(texts, lang, options);

        log.debug("Sending request to Yandex Speller:");
        log.debug("URI: {}", uri);
        log.debug("Number of texts: {}", texts.size());
        for (int i = 0; i < texts.size(); i++) {
            log.debug("Text {} (length {}): '{}'", i, texts.get(i).length(), texts.get(i));
        }

        try {
            ResponseEntity<List<List<SpellResult>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<List<SpellResult>>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Received response from Yandex Speller:");
                for (int i = 0; i < response.getBody().size(); i++) {
                    List<SpellResult> chunkResults = response.getBody().get(i);
                    log.debug("Chunk {} has {} errors", i, chunkResults.size());
                    for (SpellResult result : chunkResults) {
                        log.debug("  Error: word='{}', pos={}, len={}, suggestions={}",
                                result.getWord(), result.getPos(), result.getLen(),
                                result.getSuggestions());
                    }
                }
                return response.getBody();
            } else {
                throw new YandexSpellerException("Failed to get response from Yandex Speller API");
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Yandex Speller API HTTP error: {}", e.getMessage());
            throw new YandexSpellerException("Yandex Speller API error: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            log.error("Yandex Speller API timeout: {}", e.getMessage());
            throw new YandexSpellerException("Yandex Speller API timeout", e);
        } catch (Exception e) {
            log.error("Unexpected error in Yandex Speller: {}", e.getMessage());
            throw new YandexSpellerException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private URI buildUri(List<String> texts, String lang, int options) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl() + "/checkTexts")
                .queryParam("lang", lang)
                .queryParam("options", options);

        for (String text : texts) {
            String cleanedText = text.replace("\u200C", "");
            String encodedText = URLEncoder.encode(cleanedText, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%40", "@");
            builder.queryParam("text", encodedText);
        }

        return builder.build(true).toUri();
    }
}