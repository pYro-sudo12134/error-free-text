package by.losik.errorfreetext.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class CacheService {
    @CacheEvict(value = "tasks", key = "#taskId")
    public void evictTaskCache(UUID taskId) {
        log.debug("Evicting cache for task: {}", taskId);
    }
}