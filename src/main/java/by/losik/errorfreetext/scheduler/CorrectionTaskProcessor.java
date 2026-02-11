package by.losik.errorfreetext.scheduler;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.external.yandex.exception.YandexSpellerException;
import by.losik.errorfreetext.repository.CorrectionTaskRepository;
import by.losik.errorfreetext.service.TextCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectionTaskProcessor {

    private final TextCorrectionService textCorrectionService;
    private final CorrectionTaskRepository taskRepository;

    @Async("taskExecutor")
    @Transactional
    public void processAsync(CorrectionTask task) {
        log.debug("Async processing task: {}", task.getId());

        try {
            String correctedText = textCorrectionService.correctText(
                    task.getOriginalText(),
                    task.getLanguage()
            );

            task.setStatus(TaskStatus.COMPLETED);
            task.setCorrectedText(correctedText);
            task.setProcessedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.debug("Task {} completed successfully", task.getId());

        } catch (YandexSpellerException e) {
            log.error("Yandex API error for task {}: {}", task.getId(), e.getMessage());

            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Yandex API error: " + e.getMessage());
            task.setProcessedAt(LocalDateTime.now());
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Unexpected error processing task {}: {}", task.getId(), e.getMessage(), e);

            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Internal error: " + e.getMessage());
            task.setProcessedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}