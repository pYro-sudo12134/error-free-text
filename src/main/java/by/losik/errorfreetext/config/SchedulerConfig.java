package by.losik.errorfreetext.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerConfig {
    private int fixedDelay = 10000;
    private int batchSize = 10;
    private int poolSize = 2;
}