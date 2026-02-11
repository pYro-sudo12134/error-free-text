package by.losik.errorfreetext.dto;

import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import by.losik.errorfreetext.validation.ValidLanguage;
import by.losik.errorfreetext.validation.ValidText;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Text cannot be blank")
        @ValidText
        private String text;

        @NotNull(message = "Language is required")
        @ValidLanguage
        private Language language;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {
        private UUID taskId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GetResponse {
        private UUID taskId;
        private TaskStatus status;
        private String originalText;
        private String correctedText;
        private Language language;
        private String errorMessage;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime processedAt;
    }
}