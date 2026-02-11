package by.losik.errorfreetext.scheduler;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.service.TaskService;
import by.losik.errorfreetext.service.TextCorrectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты CorrectionTaskScheduler")
class CorrectionTaskSchedulerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private TextCorrectionService textCorrectionService;

    @InjectMocks
    private CorrectionTaskScheduler scheduler;

    private CorrectionTask testTask;
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testTask = new CorrectionTask();
        testTask.setId(taskId);
        testTask.setOriginalText("Test text");
        testTask.setLanguage(Language.EN);
        testTask.setStatus(TaskStatus.NEW);
    }

    @Test
    @DisplayName("Должен отметить задачу как FAILED при ошибке обработки, но не прерывать выполнение")
    void shouldMarkTaskAsFailedWhenProcessingFails() {
        when(taskService.getNextTaskForProcessing())
                .thenReturn(testTask)
                .thenReturn(null);

        when(taskService.markTaskAsProcessing(taskId)).thenReturn(true);

        RuntimeException apiException = new RuntimeException("Yandex API timeout");
        when(textCorrectionService.correctText(anyString(), any(Language.class)))
                .thenThrow(apiException);

        scheduler.processTasks();

        verify(taskService).markTaskAsFailed(eq(taskId),
                argThat(msg -> msg.contains("Yandex API timeout")));

        verify(taskService).markTaskAsProcessing(taskId);
        verify(textCorrectionService).correctText(anyString(), any(Language.class));
        verify(taskService, never()).markTaskAsCompleted(any(UUID.class), anyString());
    }

    @Test
    @DisplayName("Должен обработать несколько задач подряд")
    void shouldProcessMultipleTasksInSequence() {
        CorrectionTask task1 = testTask;
        CorrectionTask task2 = new CorrectionTask();
        task2.setId(UUID.randomUUID());
        task2.setOriginalText("Another text");
        task2.setLanguage(Language.RU);

        when(taskService.getNextTaskForProcessing())
                .thenReturn(task1)
                .thenReturn(task2)
                .thenReturn(null);

        when(taskService.markTaskAsProcessing(any(UUID.class)))
                .thenReturn(true);

        when(textCorrectionService.correctText(anyString(), any(Language.class)))
                .thenReturn("Corrected text");

        scheduler.processTasks();

        verify(taskService, times(2)).markTaskAsProcessing(any(UUID.class));
        verify(textCorrectionService, times(2)).correctText(anyString(), any(Language.class));
        verify(taskService, times(2)).markTaskAsCompleted(any(UUID.class), anyString());
    }
}