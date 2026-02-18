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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты TaskService")
class TaskServiceTest {

    @Mock
    private CorrectionTaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private CacheService cacheService;
    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<CorrectionTask> taskCaptor;

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
            Mockito.when(taskRepository.save(any(CorrectionTask.class))).thenReturn(testTask);
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
            Mockito.when(taskRepository.save(any(CorrectionTask.class))).thenReturn(enTask);
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
            Mockito.verify(taskMapper, Mockito.never()).toGetResponse(any());
        }
    }

    @Nested
    @DisplayName("Тесты изменения статуса задачи")
    class TaskStatusUpdateTests {

        @Test
        @DisplayName("Должен отметить задачу как COMPLETED и эвиктировать кэш")
        void shouldMarkTaskAsCompletedAndEvictCache() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.eq(correctedText),
                            any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isTrue();

            Mockito.verify(taskRepository).markAsCompleted(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(correctedText),
                    any(LocalDateTime.class)
            );

            Mockito.verify(cacheService).evictTaskCache(testTaskId);
        }

        @Test
        @DisplayName("Должен отметить задачу как FAILED и сбрасывать кэш")
        void shouldMarkTaskAsFailedAndEvictCache() {
            String errorMessage = "Ошибка обработки";
            Mockito.when(taskRepository.markAsFailed(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.eq(errorMessage),
                            any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsFailed(testTaskId, errorMessage);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsFailed(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(errorMessage),
                    any(LocalDateTime.class)
            );

            Mockito.verify(cacheService).evictTaskCache(testTaskId);
        }

        @Test
        @DisplayName("Не должен сбрасывать кэш, если задача не найдена при отметке COMPLETED")
        void shouldNotEvictCacheWhenTaskNotFoundForCompleted() {
            String correctedText = "Исправленный текст";
            Mockito.when(taskRepository.markAsCompleted(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.eq(correctedText),
                            any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsCompleted(testTaskId, correctedText);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsCompleted(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(correctedText),
                    any(LocalDateTime.class)
            );

            Mockito.verify(cacheService, Mockito.never()).evictTaskCache(any());
        }

        @Test
        @DisplayName("Не должен сбрасывать кэш, если задача не найдена при отметке FAILED")
        void shouldNotEvictCacheWhenTaskNotFoundForFailed() {
            String errorMessage = "Ошибка";
            Mockito.when(taskRepository.markAsFailed(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.eq(errorMessage),
                            any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsFailed(testTaskId, errorMessage);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsFailed(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(errorMessage),
                    any(LocalDateTime.class)
            );

            Mockito.verify(cacheService, Mockito.never()).evictTaskCache(any());
        }
    }

    @Nested
    @DisplayName("Тесты граничных случаев")
    class BoundaryTests {

        @Test
        @DisplayName("Должен обработать null correctedText и не сбрасывать кэш")
        void shouldHandleNullCorrectedTextWhenMarkingAsCompleted() {
            Mockito.when(taskRepository.markAsCompleted(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.isNull(),
                            ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(0);

            boolean result = taskService.markTaskAsCompleted(testTaskId, null);

            assertThat(result).isFalse();
            Mockito.verify(taskRepository).markAsCompleted(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.isNull(),
                    ArgumentMatchers.any(LocalDateTime.class)
            );
            Mockito.verify(cacheService, Mockito.never()).evictTaskCache(any());
        }

        @Test
        @DisplayName("Должен обработать пустой correctedText и сбрасывать кэш при успехе")
        void shouldHandleEmptyCorrectedTextWhenMarkingAsCompleted() {
            String emptyText = "";
            Mockito.when(taskRepository.markAsCompleted(
                            ArgumentMatchers.eq(testTaskId),
                            ArgumentMatchers.eq(emptyText),
                            ArgumentMatchers.any(LocalDateTime.class)))
                    .thenReturn(1);

            boolean result = taskService.markTaskAsCompleted(testTaskId, emptyText);

            assertThat(result).isTrue();
            Mockito.verify(taskRepository).markAsCompleted(
                    ArgumentMatchers.eq(testTaskId),
                    ArgumentMatchers.eq(emptyText),
                    ArgumentMatchers.any(LocalDateTime.class)
            );

            Mockito.verify(cacheService).evictTaskCache(testTaskId);
        }
    }

    @Nested
    @DisplayName("Тесты интеграции с маппером")
    class MapperIntegrationTests {

        @Test
        @DisplayName("Должен правильно маппить CreateRequest в Entity")
        void shouldMapCreateRequestToEntity() {
            Mockito.when(taskMapper.toEntity(createRequest)).thenReturn(testTask);
            Mockito.when(taskRepository.save(any(CorrectionTask.class))).thenReturn(testTask);
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