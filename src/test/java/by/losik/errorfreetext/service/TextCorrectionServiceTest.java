package by.losik.errorfreetext.service;

import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.external.yandex.client.YandexSpellerClient;
import by.losik.errorfreetext.external.yandex.model.SpellResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты TextCorrectionService")
class TextCorrectionServiceTest {

    @Mock
    private YandexSpellerClient spellerClient;

    @InjectMocks
    private TextCorrectionService textCorrectionService;

    private static final int MAX_CHUNK_SIZE = 10000;
    private static final int IGNORE_URLS = 4;
    private static final int IGNORE_DIGITS = 2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(textCorrectionService, "MAX_CHUNK_SIZE", MAX_CHUNK_SIZE);
        ReflectionTestUtils.setField(textCorrectionService, "IGNORE_URLS", IGNORE_URLS);
        ReflectionTestUtils.setField(textCorrectionService, "IGNORE_DIGITS", IGNORE_DIGITS);
    }

    @Nested
    @DisplayName("Тесты коррекции текста")
    class CorrectTextTests {

        @Test
        @DisplayName("Должен успешно исправить текст с ошибками")
        void shouldCorrectTextSuccessfully() {
            String originalText = "Привет, как дела? У меня вс хорошо, спасиба!";
            Language language = Language.RU;

            SpellResult error1 = new SpellResult();
            error1.setWord("вс");
            error1.setSuggestions(List.of("все"));

            SpellResult error2 = new SpellResult();
            error2.setWord("спасиба");
            error2.setSuggestions(List.of("спасибо"));

            when(spellerClient.checkTextsWithRetry(anyList(), eq("ru"), anyInt()))
                    .thenReturn(List.of(List.of(error1, error2)));

            String correctedText = textCorrectionService.correctText(originalText, language);

            assertThat(correctedText)
                    .contains("все")
                    .contains("спасибо")
                    .doesNotContain("вс ")
                    .doesNotContain("спасиба");
        }
    }

    @Nested
    @DisplayName("Тесты разбивки текста")
    class SplitTextTests {

        @Test
        @DisplayName("Должен разбивать по знакам препинания")
        void shouldSplitAtPunctuation() {
            String text = "Первое предложение. Второе предложение! Третье? Четвертое.";

            List<String> chunks = invokeSplitMethod(text, 20);

            assertThat(chunks).allSatisfy(chunk ->
                    assertThat(chunk).matches(".*[.!?]\\s*$|^.{1,20}$")
            );
        }

        @Test
        @DisplayName("Должен разбивать по пробелам если нет знаков препинания")
        void shouldSplitAtSpaces() {
            String text = "Это очень длинный текст который не имеет знаков препинания";

            List<String> chunks = invokeSplitMethod(text, 20);

            assertThat(chunks).allSatisfy(chunk ->
                    assertThat(chunk.length()).isLessThanOrEqualTo(25)
            );
        }

        @Test
        @DisplayName("Должен разбивать точно по границе при необходимости")
        void shouldSplitExactlyAtBoundary() {
            String text = "abcdefghijklmnopqrstuvwxyz";

            List<String> chunks = invokeSplitMethod(text, 10);

            assertThat(chunks).containsExactly(
                    "abcdefghij",
                    "klmnopqrst",
                    "uvwxyz"
            );
        }
    }

    @Nested
    @DisplayName("Тесты определения опций")
    class CalculateOptionsTests {

        @Test
        @DisplayName("Должен включить IGNORE_DIGITS при наличии цифр")
        void shouldEnableIgnoreDigits() {
            String text = "Текст с цифрой 123";
            int options = invokeCalculateOptions(text);
            assertThat(options & IGNORE_DIGITS).isEqualTo(IGNORE_DIGITS);
        }

        @Test
        @DisplayName("Должен включить IGNORE_URLS при наличии URL")
        void shouldEnableIgnoreUrls() {
            String text = "Сайт https://example.com";
            int options = invokeCalculateOptions(text);
            assertThat(options & IGNORE_URLS).isEqualTo(IGNORE_URLS);
        }
    }

    @Nested
    @DisplayName("Тесты применения исправлений")
    class ApplyCorrectionsTests {

        @Test
        @DisplayName("Должен исправить слово без изменения частей других слов")
        void shouldCorrectWithoutAffectingSubstrings() {
            String originalText = "всюду вс";

            SpellResult error = new SpellResult();
            error.setWord("вс");
            error.setSuggestions(List.of("все"));

            String result = invokeApplyCorrections(originalText,
                    List.of(originalText),
                    List.of(List.of(error)));

            assertThat(result).isEqualTo("всюду все");
        }

        @Test
        @DisplayName("Должен исправить слово с учетом регистра")
        void shouldPreserveCase() {
            String originalText = "ВС ВСЕМ";

            SpellResult error = new SpellResult();
            error.setWord("ВС");
            error.setSuggestions(List.of("ВСЕ"));

            String result = invokeApplyCorrections(originalText,
                    List.of(originalText),
                    List.of(List.of(error)));

            assertThat(result).isEqualTo("ВСЕ ВСЕМ");
        }

        @Test
        @DisplayName("Не должен исправлять части слов")
        void shouldNotCorrectSubstrings() {
            String originalText = "восстановление восстановил";

            SpellResult error = new SpellResult();
            error.setWord("восстановил");
            error.setSuggestions(List.of("восстановить"));

            String result = invokeApplyCorrections(originalText,
                    List.of(originalText),
                    List.of(List.of(error)));

            assertThat(result).isEqualTo("восстановление восстановить");
            assertThat(result).contains("восстановление");
        }
    }

    private List<String> invokeSplitMethod(String text, int maxChunkSize) {
        try {
            var method = TextCorrectionService.class.getDeclaredMethod("splitText", String.class, int.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(textCorrectionService, text, maxChunkSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int invokeCalculateOptions(String text) {
        try {
            var method = TextCorrectionService.class.getDeclaredMethod("calculateOptions", String.class);
            method.setAccessible(true);
            return (int) method.invoke(textCorrectionService, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeApplyCorrections(String originalText, List<String> chunks,
                                          List<List<SpellResult>> allResults) {
        try {
            var method = TextCorrectionService.class.getDeclaredMethod(
                    "applyCorrections", String.class, List.class, List.class);
            method.setAccessible(true);
            return (String) method.invoke(textCorrectionService, originalText, chunks, allResults);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}