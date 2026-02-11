package by.losik.errorfreetext.config;

import by.losik.errorfreetext.aspect.CacheMonitoringAspect;
import by.losik.errorfreetext.aspect.ExternalApiLoggingAspect;
import by.losik.errorfreetext.aspect.LoggingAspect;
import by.losik.errorfreetext.aspect.SchedulerLoggingAspect;
import by.losik.errorfreetext.aspect.TaskConcurrencyLimiterAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "spring.app.features.logging-aspect", havingValue = "true", matchIfMissing = true)
public class AopConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.app.logging.enabled", havingValue = "true", matchIfMissing = true)
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.app.logging.enabled", havingValue = "true", matchIfMissing = true)
    public ExternalApiLoggingAspect externalApiLoggingAspect() {
        return new ExternalApiLoggingAspect();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.app.logging.enabled", havingValue = "true", matchIfMissing = true)
    public SchedulerLoggingAspect schedulerLoggingAspect() {
        return new SchedulerLoggingAspect();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.app.logging.enabled", havingValue = "true", matchIfMissing = true)
    public TaskConcurrencyLimiterAspect taskConcurrencyLimiterAspect(
            @Value("${app.scheduler.max-concurrent-tasks:5}") int maxConcurrentTasks) {
        return new TaskConcurrencyLimiterAspect(maxConcurrentTasks);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.app.logging.enabled", havingValue = "true", matchIfMissing = true)
    public CacheMonitoringAspect cacheMonitoringAspect() {
        return new CacheMonitoringAspect();
    }
}