package by.losik.errorfreetext.scheduler;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.repository.CorrectionTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectionTaskScheduler {

    private final CorrectionTaskRepository taskRepository;
    private final CorrectionTaskProcessor taskProcessor;

    @Value("${app.scheduler.batch-size:5}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval:1000}")
    public void dispatchTasks() {
        int dispatched = 0;

        for (int i = 0; i < batchSize; i++) {
            CorrectionTask task = taskRepository.pollNextTask().orElse(null);

            if (task == null) {
                break;
            }

            taskProcessor.processAsync(task);
            dispatched++;
        }

        if (dispatched > 0) {
            log.info("Dispatched {} tasks for processing", dispatched);
        }
    }
}