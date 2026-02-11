package by.losik.errorfreetext.controller;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.exception.TaskNotFoundException;
import by.losik.errorfreetext.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@DisplayName("Тесты TaskController")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private final UUID testTaskId = UUID.fromString("27ee0fb7-e24b-4650-8cb6-ac81d20c5589");
    private TaskDto.CreateRequest validCreateRequest;
    private TaskDto.CreateResponse createResponse;
    private TaskDto.GetResponse getResponse;

    @BeforeEach
    void setUp() {
        validCreateRequest = new TaskDto.CreateRequest();
        validCreateRequest.setText("Hello world");
        validCreateRequest.setLanguage(Language.EN);

        createResponse = TaskDto.CreateResponse.builder()
                .taskId(testTaskId)
                .build();

        getResponse = TaskDto.GetResponse.builder()
                .taskId(testTaskId)
                .originalText("Hello world")
                .language(Language.EN)
                .status(TaskStatus.COMPLETED)
                .correctedText("Hello world")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/tasks - Создание задачи")
    class CreateTaskTests {

        @Test
        @DisplayName("Должен создать задачу и вернуть 201 CREATED")
        void shouldCreateTaskSuccessfully() throws Exception {
            when(taskService.createTask(any(TaskDto.CreateRequest.class)))
                    .thenReturn(createResponse);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.taskId").value(testTaskId.toString()));

            verify(taskService).createTask(any(TaskDto.CreateRequest.class));
        }

        @Test
        @DisplayName("Должен вернуть 400 при невалидном тексте")
        void shouldReturn400WhenTextIsInvalid() throws Exception {
            TaskDto.CreateRequest invalidRequest = new TaskDto.CreateRequest();
            invalidRequest.setText("a");
            invalidRequest.setLanguage(Language.EN);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }

        @Test
        @DisplayName("Должен вернуть 400 при пустом языке")
        void shouldReturn400WhenLanguageIsNull() throws Exception {
            TaskDto.CreateRequest invalidRequest = new TaskDto.CreateRequest();
            invalidRequest.setText("Hello world");
            invalidRequest.setLanguage(null);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }

        @Test
        @DisplayName("Должен вернуть 400 при пустом теле запроса")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{taskId} - Получение задачи")
    class GetTaskTests {

        @Test
        @DisplayName("Должен вернуть задачу по ID")
        void shouldReturnTaskById() throws Exception {
            when(taskService.getTask(testTaskId)).thenReturn(getResponse);

            mockMvc.perform(get("/api/tasks/{taskId}", testTaskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.taskId").value(testTaskId.toString()))
                    .andExpect(jsonPath("$.originalText").value("Hello world"))
                    .andExpect(jsonPath("$.language").value("EN"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            verify(taskService).getTask(testTaskId);
        }

        @Test
        @DisplayName("Должен вернуть 404 если задача не найдена")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(taskService.getTask(nonExistentId))
                    .thenThrow(new TaskNotFoundException("Task with id: " + nonExistentId + " not found"));

            mockMvc.perform(get("/api/tasks/{taskId}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("40401"))
                    .andExpect(jsonPath("$.errorMessage").value(containsString("not found")));

            verify(taskService).getTask(nonExistentId);
        }

        @Test
        @DisplayName("Должен вернуть 400 при невалидном UUID")
        void shouldReturn400WhenInvalidUuid() throws Exception {
            mockMvc.perform(get("/api/tasks/invalid-uuid"))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).getTask(any());
        }
    }

    @Nested
    @DisplayName("Интеграция с валидацией")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Должен вернуть 400 для текста из спецсимволов")
        void shouldReturn400ForSpecialCharactersOnly() throws Exception {
            TaskDto.CreateRequest request = new TaskDto.CreateRequest();
            request.setText("@#$%^&*");
            request.setLanguage(Language.RU);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Должен вернуть 400 для текста с цифрами без букв")
        void shouldReturn400ForDigitsOnly() throws Exception {
            TaskDto.CreateRequest request = new TaskDto.CreateRequest();
            request.setText("123456");
            request.setLanguage(Language.EN);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}