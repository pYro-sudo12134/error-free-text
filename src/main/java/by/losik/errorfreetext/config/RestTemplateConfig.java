package by.losik.errorfreetext.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(YandexSpellerConfig config) {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(config.getRequestTimeout()))
                .setReadTimeout(Duration.ofMillis(config.getRequestTimeout()))
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(config.getRequestTimeout());
                    factory.setReadTimeout(config.getRequestTimeout());
                    return factory;
                })
                .build();
    }
}