package by.losik.errorfreetext.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("tasks", Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("tasks-processing", Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES) // Короткое TTL для часто меняющихся данных
                .recordStats()
                .build());

        return cacheManager;
    }

    @Scheduled(fixedRate = 60000)
    public void logCacheStats() {
        CacheManager cacheManager = cacheManager();

        cacheManager.getCacheNames().forEach(cacheName -> {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                            cacheManager.getCache(cacheName).getNativeCache();

            CacheStats stats = nativeCache.stats();

            log.debug("Cache '{}' stats: hitRate={}%, missRate={}%, loadSuccess={}, loadFailure={}, evictionCount={}",
                    cacheName,
                    String.format("%.2f", stats.hitRate() * 100),
                    String.format("%.2f", stats.missRate() * 100),
                    stats.loadSuccessCount(),
                    stats.loadFailureCount(),
                    stats.evictionCount());
        });
    }
}