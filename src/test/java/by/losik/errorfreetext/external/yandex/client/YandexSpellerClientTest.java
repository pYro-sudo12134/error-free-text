package by.losik.errorfreetext.external.yandex.client;

import by.losik.errorfreetext.config.YandexSpellerConfig;
import by.losik.errorfreetext.external.yandex.exception.YandexSpellerException;
import by.losik.errorfreetext.external.yandex.model.SpellResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Тесты YandexSpellerClient")
class YandexSpellerClientTest {

    private MockWebServer mockWebServer;
    private YandexSpellerClient spellerClient;
    private ObjectMapper objectMapper;

    @Mock
    private YandexSpellerConfig config;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(0,
                new MappingJackson2HttpMessageConverter(objectMapper));

        when(config.getBaseUrl()).thenReturn(mockWebServer.url("/").toString());

        spellerClient = new YandexSpellerClient(restTemplate, config);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("Тесты успешных сценариев")
    class SuccessTests {

        @Test
        @DisplayName("Должен успешно обработать один текст без ошибок")
        void shouldProcessSingleTextWithoutErrors() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Hello world");

            List<List<SpellResult>> result = spellerClient.checkTextsWithRetry(
                    texts, "en", 0
            );

            assertThat(result).isNotNull().hasSize(1).allMatch(List::isEmpty);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).contains("/checkTexts");
            assertThat(request.getPath()).contains("lang=en");
            assertThat(request.getPath()).contains("options=0");
            assertThat(request.getPath()).containsAnyOf("text=Hello world", "text=Hello%20world", "text=Hello+world");
        }

        @Test
        @DisplayName("Должен успешно обработать текст с ошибками")
        void shouldProcessTextWithErrors() throws Exception {
            List<SpellResult> errors = List.of(
                    SpellResult.builder()
                            .word("tanks")
                            .suggestions(List.of("thanks", "tanks"))
                            .build()
            );
            List<List<SpellResult>> mockResponse = List.of(errors);
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("tanks for help");

            List<List<SpellResult>> result = spellerClient.checkTextsWithRetry(
                    texts, "en", 0
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).hasSize(1);
            assertThat(result.get(0).get(0).getWord()).isEqualTo("tanks");
            assertThat(result.get(0).get(0).getSuggestions())
                    .containsExactly("thanks", "tanks");
        }

        @Test
        @DisplayName("Должен успешно обработать несколько текстов")
        void shouldProcessMultipleTexts() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(
                    List.of(),
                    List.of(SpellResult.builder().word("speling").suggestions(List.of("spelling")).build())
            );
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Hello world", "Bad speling");

            List<List<SpellResult>> result = spellerClient.checkTextsWithRetry(
                    texts, "en", 0
            );

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEmpty();
            assertThat(result.get(1)).hasSize(1);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).containsAnyOf("text=Hello world", "text=Hello%20world", "text=Hello+world");
            assertThat(request.getPath()).containsAnyOf("text=Bad speling", "text=Bad%20speling", "text=Bad+speling");
        }

        @Test
        @DisplayName("Должен корректно обрабатывать опции IGNORE_DIGITS")
        void shouldHandleIgnoreDigitsOption() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Text with 123 digits");
            int options = 2;

            spellerClient.checkTextsWithRetry(texts, "en", options);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("options=2");
        }

        @Test
        @DisplayName("Должен корректно обрабатывать опции IGNORE_URLS")
        void shouldHandleIgnoreUrlsOption() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Check https://example.com");
            int options = 4;

            spellerClient.checkTextsWithRetry(texts, "en", options);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("options=4");
        }

        @Test
        @DisplayName("Должен корректно обрабатывать специальные символы")
        void shouldHandleSpecialCharacters() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Текст с @спец#символами! $%");

            spellerClient.checkTextsWithRetry(texts, "ru", 0);

            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();

            assertThat(path).contains("@");
            assertThat(path).doesNotContain("%40");

            assertThat(path).contains("%23");
            assertThat(path).contains("%20");
            assertThat(path).contains("%24");
            assertThat(path).contains("%25");

            assertThat(path).contains("%D0%A2%D0%B5%D0%BA%D1%81%D1%82");
        }

        @Test
        @DisplayName("Должен удалять символы нулевой ширины")
        void shouldRemoveZeroWidthCharacters() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Text with\u200Czero width");

            spellerClient.checkTextsWithRetry(texts, "en", 0);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).doesNotContain("\u200C");
            assertThat(request.getPath()).containsAnyOf("zero width", "zero%20width", "zero+width");
        }
    }

    @Nested
    @DisplayName("Тесты обработки ошибок")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Должен выбросить YandexSpellerException при 400 ошибке")
        void shouldThrowExceptionOnBadRequest() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.BAD_REQUEST.value())
                    .setBody("Bad Request"));

            List<String> texts = List.of("Hello");

            assertThatThrownBy(() ->
                    spellerClient.checkTextsWithRetry(texts, "en", 0)
            )
                    .isInstanceOf(YandexSpellerException.class)
                    .hasMessageContaining("Yandex Speller API error");
        }

        @Test
        @DisplayName("Должен выбросить YandexSpellerException при 500 ошибке")
        void shouldThrowExceptionOnServerError() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setBody("Internal Server Error"));

            List<String> texts = List.of("Hello");

            assertThatThrownBy(() ->
                    spellerClient.checkTextsWithRetry(texts, "en", 0)
            )
                    .isInstanceOf(YandexSpellerException.class)
                    .hasMessageContaining("Yandex Speller API error");
        }

        @Test
        @DisplayName("Должен выбросить YandexSpellerException при таймауте")
        void shouldThrowExceptionOnTimeout() {
            mockWebServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.NO_RESPONSE));

            List<String> texts = List.of("Hello");

            assertThatThrownBy(() ->
                    spellerClient.checkTextsWithRetry(texts, "en", 0)
            )
                    .isInstanceOf(YandexSpellerException.class)
                    .hasMessageContaining("timeout");
        }

        @Test
        @DisplayName("Должен выбросить исключение при пустом ответе")
        void shouldThrowExceptionOnNullResponseBody() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value()));

            List<String> texts = List.of("Hello");

            assertThatThrownBy(() ->
                    spellerClient.checkTextsWithRetry(texts, "en", 0)
            )
                    .isInstanceOf(YandexSpellerException.class)
                    .hasMessageContaining("Failed to get response");
        }

        @Test
        @DisplayName("Должен выбросить исключение при некорректном JSON")
        void shouldThrowExceptionOnInvalidJson() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody("invalid json")
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Hello");

            assertThatThrownBy(() ->
                    spellerClient.checkTextsWithRetry(texts, "en", 0)
            )
                    .isInstanceOf(YandexSpellerException.class);
        }
    }

    @Nested
    @DisplayName("Тесты URL построения")
    class UrlBuildingTests {

        @Test
        @DisplayName("Должен корректно строить URL с одним текстом")
        void shouldBuildUrlWithSingleText() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Test text");

            spellerClient.checkTextsWithRetry(texts, "ru", 0);

            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();
            assertThat(path).startsWith("/checkTexts?lang=ru&options=0&text=");
            assertThat(path).containsAnyOf("text=Test text", "text=Test%20text", "text=Test+text");
        }

        @Test
        @DisplayName("Должен корректно строить URL с несколькими текстами")
        void shouldBuildUrlWithMultipleTexts() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of(), List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("First", "Second", "Third");

            spellerClient.checkTextsWithRetry(texts, "en", 4);

            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();
            assertThat(path).contains("lang=en");
            assertThat(path).contains("options=4");
            assertThat(path).contains("text=First");
            assertThat(path).contains("text=Second");
            assertThat(path).contains("text=Third");
        }

        @Test
        @DisplayName("Должен корректно кодировать кириллицу")
        void shouldEncodeCyrillicText() throws Exception {
            List<List<SpellResult>> mockResponse = List.of(List.of());
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of("Привет мир");

            spellerClient.checkTextsWithRetry(texts, "ru", 0);

            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();

            String expectedPart = URLEncoder.encode("Привет мир", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            assertThat(path).contains("text=" + expectedPart);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать пустой список текстов")
        void shouldHandleEmptyTextList() throws Exception {
            List<List<SpellResult>> mockResponse = List.of();
            String responseBody = objectMapper.writeValueAsString(mockResponse);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"));

            List<String> texts = List.of();

            List<List<SpellResult>> result = spellerClient.checkTextsWithRetry(texts, "en", 0);

            assertThat(result).isEmpty();
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).doesNotContain("&text=");
        }
    }
}