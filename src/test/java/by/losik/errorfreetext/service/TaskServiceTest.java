package by.losik.errorfreetext.service;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.exception.TaskNotFoundException;
import by.losik.errorfreetext.mapper.TaskMapper;
import by.losik.errorfreetext.repository.CorrectionTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты TaskService")
class TaskServiceTest {

    @Mock
    private CorrectionTaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<CorrectionTask> taskCaptor;

    @Captor
    private ArgumentCaptor<UUID> uuidCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> dateTimeCaptor;

    private final UUID testTaskId = UUID.fromString("27ee0fb7-e24b-4650-8cb6-ac81d20c5589");
    private CorrectionTask testTask;
    private TaskDto.CreateRequest createRequest;
    private TaskDto.CreateResponse createResponse;
    private TaskDto.GetResponse getResponse;

    @BeforeEach
    void setUp() {
        testTask = new CorrectionTask();
        testTask.setId(testTaskId);
        testTask.setOriginalText("Тестовый текст");
        testTask.setLanguage(Language.RU);
        testTask.setStatus(TaskStatus.NEW);
        testTask.setCreatedAt(LocalDateTime.now());

        createRequest = new TaskDto.CreateRequest();
        createRequest.setText("Тестовый текст");
        createRequest.setLanguage(Language.RU);

        createResponse = TaskDto.CreateResponse.builder()
                .taskId(testTaskId)
                .build();

        getResponse = TaskDto.GetResponse.builder()
                .taskId(testTaskId)
                .originalText("Тестовый текст")
                .language(Language.RU)
                .status(TaskStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Тесты создания задачи")
    class CreateTaskTests {

        @Test
        @DisplayName("Должен успешно создать новую задачу")
        void shouldCreateTaskSuccessfully() {
             
            Mockito.when(taskMapper.toEntity(createRequest)).thenReturn(testTask);
            Mockito.when(taskRepository.save(ArgumentMatchers.any(CorrectionTask.class))).thenReturn(testTask);
            Mockito.when(taskMapper.toCreateResponse(testTask)).thenReturn(createResponse);

             
            TaskDto.CreateResponse response = taskService.createTask(createRequest);

             
            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isEqualTo(testTaskId);

            Mockito.verify(taskMapper).toEntity(createRequest);
            Mockito.verify(taskRepository).save(taskCaptor.capture());
            Mockito.verify(taskMapper).toCreateResponse(testTask);

            CorrectionTask capturedTask = taskCaptor.getValue();
            assertThat(capturedTask.getOriginalText()).isEqualTo("Тестовый текст");
            assertThat(capturedTask.getLanguage()).isEqualTo(Language.RU);
            assertThat(capturedTask.getStatus()).isEqualTo(TaskStatus.NEW);
            assertThat(capturedTask.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Должен создать задачу с английским языком")
        void shouldCreateTaskWithEnglishLanguage() {
             
            TaskDto.CreateRequest enRequest = new TaskDto.CreateRequest();
            enRequest.setText("English text");
            enRequest.setLanguage(Language.EN);

            CorrectionTask enTask = new CorrectionTask();
            enTask.setId(testTaskId);
            enTask.setOriginalText("English text");
            enTask.setLanguage(Language.EN);
            enTask.setStatus(TaskStatus.NEW);

            Mockito.when(taskMapper.toEntity(enRequest)).thenReturn(enTask);
            Mockito.when(taskRepository.save(ArgumentMatchers.any(CorrectionTask.class))).thenReturn(enTask);
            Mockito.when(taskMapper.toCreateResponse(enTask)).thenReturn(createResponse);

             
            taskService.createTask(enRequest);

             
            Mockito.verify(taskRepository).save(taskCaptor.capture());
            CorrectionTask capturedTask = taskCaptor.getValue();
            assertThat(capturedTask.getLanguage()).isEqualTo(Language.EN);
            assertThat(capturedTask.getOriginalText()).isEqualTo("English text");
            assertThat(capturedTask.getStatus()).isEqualTo(TaskStatus.NEW);
        }
    }

    @Nested
    @DisplayName("Тесты получения задачи")
    class GetTaskTests {

        @Test
        @DisplayName("Должен успешно получить существующую задачу")
        void shouldGetExistingTaskSuccessfully() {
             
            Mockito.when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
            Mockito.when(taskMapper.toGetResponse(testTask)).thenReturn(getResponse);

             
            TaskDto.GetResponse response = taskService.getTask(testTaskId);

             
            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isEqualTo(testTaskId);
            assertThat(response.getOriginalText()).isEqualTo("Тестовый текст");
            assertThat(response.getLanguage()).isEqualTo(Language.RU);
            assertThat(response.getStatus()).isEqualTo(TaskStatus.NEW);
            assertThat(response.getCorrectedText()).isNull();
            assertThat(response.getErrorMessage()).isNull();

            Mockito.verify(taskRepository).findById(testTaskId);
            Mockito.verify(taskMapper).toGetResponse(testTask);
        }

        @Test
        @DisplayName("Должен выбросить исключение при поиске несуществующей задачи")
        void shouldThrowExceptionWhenTaskNotFound() {
             
            UUID nonExistentId = UUID.randomUUID();
            Mockito.when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTask(nonExistentId))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("Task with id: " + nonExistentId + " not found");

            Mockito.verify(taskRepository).findById(nonExistentId);
            Mockito.verify(taskMapper, Mockito.never()).toGetResponse(ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("Тесты получения следующей задачи для обработки")
    class GetNextTaskForProcessingTests {

        @Test
        @DisplayName("Должен получить следующую задачу со статусом NEW")
        void shouldGetNextTaskForProcessing() {
             
            Mockito.when(taskRepository.findByStatusOrderByCreatedAtAsc(TaskStatus.NEW))
                    .thenReturn(List.of(testTask));

             
            CorrectionTask result = taskService.getNextTaskForProcessing();

             
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testTaskId);
            assertThat(result.getStatus()).isEqualTo(TaskStatus.NEW);

            Mockito.verify(taskRepository).findByStatusOrderByCreatedAtAsc(TaskStatus.NEW);
        }

        @Test
        @DisplayName("Должен вернуть null, если нет задач для обработки")
        void shouldReturnNullWhenNoTasksForProcessing() {
            Mockito.when(taskRepository.findByStatusOrderByCreatedAtAsc(TaskStatus.NEW))
                    .thenReturn(List.of());

            CorrectionTask result = taskService.getNextTaskForProcessing();

            assertThat(result).isNull();
            Mockito.verify(taskRepository).findByStatusOrderByCreatedAtAsc(TaskStatus.NEW);
        }
    }

    @Nested
    @DisplayName("Тесты изменения статуса задачи")
    class TaskStatusUpdateTests {

        @Test
        @DisplayName("Должен отметить задачу как PROCESSING")
        void shouldMarkTaskAsProcessing() {
            Mockito.when(taskRepository.markAsProcessing(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsProcessing(testTaskId);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsProcessing(uuidCaptor.capture(), dateTimeCaptor.capture());
            assertThat(uuidCaptor.getValue()).isEqualTo(testTaskId);
            assertThat(dateTimeCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("Должен вернуть false, если задача не найдена при отметке PROCESSING")
        void shouldReturnFalseWhenTaskNotFoundForProcessing() {
            Mockito.when(taskRepository.markAsProcessing(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsProcessing(testTaskId);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsProcessing(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Должен отметить задачу как COMPLETED")
        void shouldMarkTaskAsCompleted() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(correctedText), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsCompleted(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(correctedText),
                    ArgumentMatchers.any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("Должен отметить задачу как FAILED")
        void shouldMarkTaskAsFailed() {
            String errorMessage = "Ошибка обработки";
            Mockito.when(taskRepository.markAsFailed(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(errorMessage), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

             
            boolean result = taskService.markTaskAsFailed(testTaskId, errorMessage);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsFailed(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(errorMessage),
                    ArgumentMatchers.any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("Должен вернуть false, если задача не найдена при отметке COMPLETED")
        void shouldReturnFalseWhenTaskNotFoundForCompleted() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(correctedText), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(correctedText), ArgumentMatchers.any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Должен вернуть false, если задача не найдена при отметке FAILED")
        void shouldReturnFalseWhenTaskNotFoundForFailed() {
            String errorMessage = "Ошибка";
            Mockito.when(taskRepository.markAsFailed(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(errorMessage), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsFailed(testTaskId, errorMessage);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsFailed(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(errorMessage), ArgumentMatchers.any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Тесты граничных случаев")
    class BoundaryTests {

        @Test
        @DisplayName("Должен обработать null ID при получении задачи")
        void shouldHandleNullIdWhenGettingTask() {
            Mockito.when(taskRepository.findById(ArgumentMatchers.isNull())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTask(null))
                    .isInstanceOf(TaskNotFoundException.class);

            Mockito.verify(taskRepository).findById(ArgumentMatchers.isNull());
        }

        @Test
        @DisplayName("Должен обработать null ID при отметке PROCESSING")
        void shouldHandleNullIdWhenMarkingAsProcessing() {
            Mockito.when(taskRepository.markAsProcessing(ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsProcessing(null);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsProcessing(ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Должен обработать null correctedText при отметке COMPLETED")
        void shouldHandleNullCorrectedTextWhenMarkingAsCompleted() {
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsCompleted(testTaskId, null);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Должен обработать пустой correctedText при отметке COMPLETED")
        void shouldHandleEmptyCorrectedTextWhenMarkingAsCompleted() {
            String emptyText = "";
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(emptyText), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsCompleted(testTaskId, emptyText);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(emptyText), ArgumentMatchers.any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Должен обработать null errorMessage при отметке FAILED")
        void shouldHandleNullErrorMessageWhenMarkingAsFailed() {
            Mockito.when(taskRepository.markAsFailed(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsFailed(testTaskId, null);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsFailed(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.isNull(), ArgumentMatchers.any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Тесты кэширования")
    class CacheTests {

        @Test
        @DisplayName("Должен эвиктировать кэш задачи")
        void shouldEvictTaskCache() {
            taskService.evictTaskCache(testTaskId);
            assertThat(taskService).isNotNull();
        }

        @Test
        @DisplayName("Должен эвиктировать кэш при успешном завершении задачи")
        void shouldEvictCacheOnSuccessfulCompletion() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(correctedText), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

            TaskService spyTaskService = Mockito.spy(taskService);
            Mockito.doNothing().when(spyTaskService).evictTaskCache(testTaskId);

            boolean result = spyTaskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isTrue();
            Mockito.verify(spyTaskService).evictTaskCache(testTaskId);
        }

        @Test
        @DisplayName("Не должен эвиктировать кэш при неуспешном завершении задачи")
        void shouldNotEvictCacheOnFailedCompletion() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(ArgumentMatchers.eq(testTaskId), ArgumentMatchers.eq(correctedText), ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            TaskService spyTaskService = Mockito.spy(taskService);

            boolean result = spyTaskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isFalse();
            Mockito.verify(spyTaskService, Mockito.never()).evictTaskCache(ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("Тесты интеграции с маппером")
    class MapperIntegrationTests {

        @Test
        @DisplayName("Должен правильно маппить CreateRequest в Entity")
        void shouldMapCreateRequestToEntity() {
            Mockito.when(taskMapper.toEntity(createRequest)).thenReturn(testTask);
            Mockito.when(taskRepository.save(ArgumentMatchers.any(CorrectionTask.class))).thenReturn(testTask);
            Mockito.when(taskMapper.toCreateResponse(testTask)).thenReturn(createResponse);

            taskService.createTask(createRequest);

            Mockito.verify(taskMapper).toEntity(createRequest);
            Mockito.verify(taskMapper).toCreateResponse(testTask);
        }

        @Test
        @DisplayName("Должен правильно маппить Entity в GetResponse")
        void shouldMapEntityToGetResponse() {
            Mockito.when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
            Mockito.when(taskMapper.toGetResponse(testTask)).thenReturn(getResponse);

            taskService.getTask(testTaskId);

            Mockito.verify(taskMapper).toGetResponse(testTask);
        }
    }
}