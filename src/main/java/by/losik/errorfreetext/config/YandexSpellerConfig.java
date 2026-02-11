package by.losik.errorfreetext.config;

import lombok.Setter;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.yandex-speller")
public class YandexSpellerConfig {
    private String baseUrl = "https://speller.yandex.net/services/spellservice.json";
    private int maxTextLength = 10000;
    private int requestTimeout = 5000;
}