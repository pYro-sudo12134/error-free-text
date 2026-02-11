package by.losik.errorfreetext.integration;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.external.yandex.client.YandexSpellerClient;
import by.losik.errorfreetext.external.yandex.exception.YandexSpellerException;
import by.losik.errorfreetext.external.yandex.model.SpellResult;
import by.losik.errorfreetext.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Интеграционный тест полного цикла")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ErrorFreeTextIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.scheduler.fixed-delay", () -> "100");
        registry.add("spring.cache.type", () -> "none");
    }

    @MockBean
    private YandexSpellerClient yandexSpellerClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        taskService.evictAllCaches();
    }

    @Test
    @DisplayName("Полный цикл: создание, автоматическая обработка, получение исправленного текста")
    void fullFlowTest() {
        List<SpellResult> errors = List.of(
                SpellResult.builder()
                        .word("спсиба")
                        .suggestions(List.of("спасибо", "спасиба", "спосибо"))
                        .pos(8)
                        .len(6)
                        .build()
        );

        when(yandexSpellerClient.checkTextsWithRetry(
                any(), eq("ru"), anyInt()
        )).thenReturn(List.of(errors));

        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setText("Привет, спсиба!");
        request.setLanguage(Language.RU);

        ResponseEntity<TaskDto.CreateResponse> createResponse = restTemplate
                .postForEntity("/api/tasks", request, TaskDto.CreateResponse.class);

        UUID taskId = createResponse.getBody().getTaskId();

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<TaskDto.GetResponse> response = restTemplate
                            .getForEntity("/api/tasks/{id}", TaskDto.GetResponse.class, taskId);
                    assertThat(response.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);
                    assertThat(response.getBody().getCorrectedText()).contains("спасибо");
                });
    }

    @Test
    @DisplayName("Должен отметить задачу как FAILED при ошибке API")
    void shouldMarkAsFailedOnApiError() {
        when(yandexSpellerClient.checkTextsWithRetry(
                anyList(), anyString(), anyInt()
        )).thenThrow(new YandexSpellerException("API error: 503 Service Unavailable"));

        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setText("Привет, спсиба!");
        request.setLanguage(Language.RU);

        ResponseEntity<TaskDto.CreateResponse> createResponse = restTemplate
                .postForEntity("/api/tasks", request, TaskDto.CreateResponse.class);

        UUID taskId = createResponse.getBody().getTaskId();

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<TaskDto.GetResponse> response = restTemplate
                            .getForEntity("/api/tasks/{id}", TaskDto.GetResponse.class, taskId);
                    assertThat(response.getBody().getStatus()).isEqualTo(TaskStatus.FAILED);
                    assertThat(response.getBody().getErrorMessage()).contains("503");
                });
    }
}