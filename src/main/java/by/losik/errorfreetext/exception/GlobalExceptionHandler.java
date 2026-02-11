package by.losik.errorfreetext.exception;

import by.losik.errorfreetext.util.EnumUtils;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MethodNotAllowedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String errorMessage = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Method argument type mismatch: {}", errorMessage);

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage(errorMessage)
                .errorCode("40003")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String errorMessage = "Invalid request body";

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            if (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum()) {
                if (invalidFormatEx.getPath() != null && !invalidFormatEx.getPath().isEmpty()) {
                    errorMessage = String.format("Invalid value '%s' for field '%s'. Allowed values: %s",
                            invalidFormatEx.getValue(),
                            invalidFormatEx.getPath().get(0).getFieldName(),
                            getEnumValues(invalidFormatEx.getTargetType()));
                } else {
                    errorMessage = String.format("Invalid value '%s' for enum type. Allowed values: %s",
                            invalidFormatEx.getValue(),
                            getEnumValues(invalidFormatEx.getTargetType()));
                }
            }
        }

        log.warn("Invalid request: {}", errorMessage);

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage(errorMessage)
                .errorCode("40002")
                .timestamp(LocalDateTime.now())
                .path(getPathSafely(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            MethodNotAllowedException ex, HttpServletRequest request) {

        log.warn("Invalid request: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage("Method not allowed for this endpoint")
                .errorCode("40501")
                .timestamp(LocalDateTime.now())
                .path(getPathSafely(request))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFoundException(
            TaskNotFoundException ex, HttpServletRequest request) {

        log.warn("Task not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage(ex.getMessage())
                .errorCode("40401")
                .timestamp(LocalDateTime.now())
                .path(getPathSafely(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        String errorMessage = "Validation failed: " + errors;
        log.warn("Validation error: {}", errorMessage);

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage(errorMessage)
                .errorCode("40001")
                .timestamp(LocalDateTime.now())
                .path(getPathSafely(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Internal server error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorMessage("Internal server error")
                .errorCode("50001")
                .timestamp(LocalDateTime.now())
                .path(getPathSafely(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getPathSafely(HttpServletRequest request) {
        try {
            return request != null ? request.getRequestURI() : null;
        } catch (Exception e) {
            log.debug("Failed to get request URI: {}", e.getMessage());
            return null;
        }
    }

    private String getEnumValues(Class<?> enumClass) {
        return EnumUtils.getEnumValuesSafe(enumClass);
    }
}