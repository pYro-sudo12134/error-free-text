package by.losik.errorfreetext.service;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.exception.TaskNotFoundException;
import by.losik.errorfreetext.mapper.TaskMapper;
import by.losik.errorfreetext.repository.CorrectionTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final CorrectionTaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CacheService cacheService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#result.taskId"),
            @CacheEvict(value = "tasks-processing", allEntries = true)
    })
    public TaskDto.CreateResponse createTask(TaskDto.CreateRequest request) {
        CorrectionTask task = taskMapper.toEntity(request);
        CorrectionTask savedTask = taskRepository.save(task);
        log.debug("Task created with ID: {}", savedTask.getId());
        return taskMapper.toCreateResponse(savedTask);
    }

    @Transactional
    @Cacheable(value = "tasks", key = "#taskId", unless = "#result == null")
    public TaskDto.GetResponse getTask(UUID taskId) {
        CorrectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id: " + taskId + " not found"));

        return taskMapper.toGetResponse(task);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#taskId"),
            @CacheEvict(value = "tasks-processing", allEntries = true)
    })
    public boolean markTaskAsCompleted(UUID taskId, String correctedText) {
        log.debug("Marking task as completed: {}", taskId);

        int updated = taskRepository.markAsCompleted(taskId, correctedText, LocalDateTime.now());
        boolean success = updated > 0;

        if (success) {
            log.debug("Task marked as completed: {}", taskId);
            cacheService.evictTaskCache(taskId);
        }

        return success;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#taskId"),
            @CacheEvict(value = "tasks-processing", allEntries = true)
    })
    public boolean markTaskAsFailed(UUID taskId, String errorMessage) {
        log.debug("Marking task as failed: {}", taskId);

        int updated = taskRepository.markAsFailed(taskId, errorMessage, LocalDateTime.now());
        boolean success = updated > 0;

        if (success) {
            log.debug("Task marked as failed: {}", taskId);
            cacheService.evictTaskCache(taskId);
        }

        return success;
    }
}