package by.losik.errorfreetext.scheduler;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.service.TaskService;
import by.losik.errorfreetext.service.TextCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectionTaskScheduler {

    private final TaskService taskService;
    private final TextCorrectionService textCorrectionService;

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay:10000}")
    public void processTasks() {
        CorrectionTask task = taskService.getNextTaskForProcessing();
        log.debug("Starting scheduler cycle, found task: {}", task != null ? task.getId() : "none");

        while (task != null) {
            try {
                processSingleTask(task);
            } catch (Exception e) {
                log.error("Failed to process task: " + task.getId(), e);
            }
            task = taskService.getNextTaskForProcessing();
            log.debug("Next task: {}", task != null ? task.getId() : "none");
        }
    }

    @Transactional
    public void processSingleTask(CorrectionTask task) {
        log.debug("Processing task: {}", task.getId());

        if (!taskService.markTaskAsProcessing(task.getId())) {
            log.warn("Could not mark task as processing: {}", task.getId());
            return;
        }

        try {
            String correctedText = textCorrectionService.correctText(
                    task.getOriginalText(),
                    task.getLanguage()
            );

            taskService.markTaskAsCompleted(task.getId(), correctedText);
            log.debug("Successfully processed task: {}", task.getId());

        } catch (Exception e) {
            log.error("Error processing task: " + task.getId(), e);
            taskService.markTaskAsFailed(task.getId(),
                    "Processing failed: " + e.getMessage());
        }
    }
}