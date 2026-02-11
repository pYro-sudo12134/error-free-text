package by.losik.errorfreetext.exception;

import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.util.EnumUtils;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MethodNotAllowedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    private final String requestPath = "/api/tasks";

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(requestPath);
    }

    @Nested
    @DisplayName("Тесты обработки HttpMessageNotReadableException")
    class HttpMessageNotReadableExceptionTests {

        @Test
        @DisplayName("Должен обработать общую ошибку чтения тела запроса")
        void shouldHandleGenericHttpMessageNotReadableException() {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Invalid JSON");

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleHttpMessageNotReadableException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage()).isEqualTo("Invalid request body");
            assertThat(response.getBody().getErrorCode()).isEqualTo("40002");
            assertThat(response.getBody().getPath()).isEqualTo(requestPath);
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Должен обработать ошибку неверного значения enum")
        void shouldHandleInvalidEnumValueException() {
            String invalidValue = "INVALID";
            String fieldName = "language";
            String allowedValues = "EN, RU";

            InvalidFormatException invalidFormatEx =
                    InvalidFormatException.from(null, "Invalid value", invalidValue, Language.class);

            JsonMappingException.Reference reference =
                    new JsonMappingException.Reference(null, fieldName);
            invalidFormatEx.prependPath(reference);

            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Invalid JSON", invalidFormatEx);

            try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
                enumUtils.when(() -> EnumUtils.getEnumValuesSafe(Language.class))
                        .thenReturn(allowedValues);

                ResponseEntity<ErrorResponse> response =
                        exceptionHandler.handleHttpMessageNotReadableException(ex, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getErrorMessage())
                        .isEqualTo(String.format("Invalid value '%s' for field '%s'. Allowed values: %s",
                                invalidValue, fieldName, allowedValues));
                assertThat(response.getBody().getErrorCode()).isEqualTo("40002");
            }
        }

        @Test
        @DisplayName("Должен обработать ошибку с пустым путем в InvalidFormatException")
        void shouldHandleInvalidFormatExceptionWithEmptyPath() {
            InvalidFormatException invalidFormatEx =
                    InvalidFormatException.from(null, "Invalid value", "test", Language.class);

            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Invalid JSON", invalidFormatEx);

            try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
                enumUtils.when(() -> EnumUtils.getEnumValuesSafe(Language.class))
                        .thenReturn("RU, EN");

                ResponseEntity<ErrorResponse> response =
                        exceptionHandler.handleHttpMessageNotReadableException(ex, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getErrorMessage())
                        .isEqualTo("Invalid value 'test' for enum type. Allowed values: RU, EN");
                assertThat(response.getBody().getErrorCode()).isEqualTo("40002");
            }
        }
    }

    @Nested
    @DisplayName("Тесты обработки MethodNotAllowedException")
    class MethodNotAllowedExceptionTests {

        @Test
        @DisplayName("Должен обработать MethodNotAllowedException")
        void shouldHandleMethodNotAllowedException() {
            MethodNotAllowedException ex =
                    new MethodNotAllowedException("POST", List.of(HttpMethod.GET));

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleMethodNotAllowed(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage())
                    .isEqualTo("Method not allowed for this endpoint");
            assertThat(response.getBody().getErrorCode()).isEqualTo("40501");
            assertThat(response.getBody().getPath()).isEqualTo(requestPath);
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Тесты обработки TaskNotFoundException")
    class TaskNotFoundExceptionTests {

        @Test
        @DisplayName("Должен обработать TaskNotFoundException")
        void shouldHandleTaskNotFoundException() {
            String errorMessage = "Task with id: 123 not found";
            TaskNotFoundException ex = new TaskNotFoundException(errorMessage);

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleTaskNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage()).isEqualTo(errorMessage);
            assertThat(response.getBody().getErrorCode()).isEqualTo("40401");
            assertThat(response.getBody().getPath()).isEqualTo(requestPath);
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Должен обработать TaskNotFoundException с null сообщением")
        void shouldHandleTaskNotFoundExceptionWithNullMessage() {
            TaskNotFoundException ex = new TaskNotFoundException(null);

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleTaskNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Тесты обработки MethodArgumentNotValidException")
    class MethodArgumentNotValidExceptionTests {

        @Test
        @DisplayName("Должен обработать ошибки валидации с одним полем")
        void shouldHandleSingleValidationError() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError = new FieldError("object", "text", "Text cannot be blank");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleValidationExceptions(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage())
                    .contains("Validation failed: {text=Text cannot be blank}");
            assertThat(response.getBody().getErrorCode()).isEqualTo("40001");
            assertThat(response.getBody().getPath()).isEqualTo(requestPath);
        }

        @Test
        @DisplayName("Должен обработать ошибки валидации с несколькими полями")
        void shouldHandleMultipleValidationErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError textError = new FieldError("object", "text", "Text cannot be blank");
            FieldError langError = new FieldError("object", "language", "Language is required");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(textError, langError));

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleValidationExceptions(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            String errorMessage = response.getBody().getErrorMessage();
            assertThat(errorMessage).contains("text=Text cannot be blank");
            assertThat(errorMessage).contains("language=Language is required");
        }
    }

    @Nested
    @DisplayName("Тесты обработки общих исключений")
    class GenericExceptionTests {

        @Test
        @DisplayName("Должен обработать RuntimeException")
        void shouldHandleRuntimeException() {
            RuntimeException ex = new RuntimeException("Database connection failed");

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleGenericException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage())
                    .isEqualTo("Internal server error");
            assertThat(response.getBody().getErrorCode()).isEqualTo("50001");
            assertThat(response.getBody().getPath()).isEqualTo(requestPath);
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Должен обработать NullPointerException")
        void shouldHandleNullPointerException() {
            NullPointerException ex = new NullPointerException();

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleGenericException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorMessage()).isEqualTo("Internal server error");
        }

        @Test
        @DisplayName("Должен обработать Exception когда request.getRequestURI() бросает исключение")
        void shouldHandleExceptionWhenRequestUriThrowsException() {
            Exception ex = new Exception("Error");
            when(request.getRequestURI()).thenThrow(new RuntimeException("URI error"));

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleGenericException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isNull();
        }
    }

    @Nested
    @DisplayName("Тесты вспомогательных методов")
    class HelperMethodTests {

        @Test
        @DisplayName("Должен получить значения enum через EnumUtils")
        void shouldGetEnumValues() {
            String expectedValues = "EN, RU";
            String invalidValue = "INVALID";
            String fieldName = "language";

            InvalidFormatException invalidFormatEx =
                    InvalidFormatException.from(null, "Invalid value", invalidValue, Language.class);

            JsonMappingException.Reference reference =
                    new JsonMappingException.Reference(null, fieldName);
            invalidFormatEx.prependPath(reference);

            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Invalid JSON", invalidFormatEx);

            try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
                enumUtils.when(() -> EnumUtils.getEnumValuesSafe(Language.class))
                        .thenReturn(expectedValues);

                ResponseEntity<ErrorResponse> response =
                        exceptionHandler.handleHttpMessageNotReadableException(ex, request);

                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getErrorMessage())
                        .contains(String.format("Invalid value '%s' for field '%s'. Allowed values: %s",
                                invalidValue, fieldName, expectedValues));
                enumUtils.verify(() -> EnumUtils.getEnumValuesSafe(Language.class));
            }
        }
    }

    @Nested
    @DisplayName("Тесты обработки MethodArgumentTypeMismatchException")
    class MethodArgumentTypeMismatchExceptionTests {

        @Test
        @DisplayName("Должен вернуть 400 при невалидном UUID")
        void shouldReturn400ForInvalidUuid() {
            MethodArgumentTypeMismatchException ex =
                    mock(MethodArgumentTypeMismatchException.class);
            when(ex.getValue()).thenReturn("invalid-uuid");
            when(ex.getName()).thenReturn("taskId");
            when(ex.getRequiredType()).thenReturn((Class) UUID.class);
            when(request.getRequestURI()).thenReturn("/api/tasks/invalid-uuid");

            ResponseEntity<ErrorResponse> response =
                    exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("40003");
            assertThat(response.getBody().getErrorMessage())
                    .contains("Invalid value 'invalid-uuid' for parameter 'taskId'");
        }
    }
}