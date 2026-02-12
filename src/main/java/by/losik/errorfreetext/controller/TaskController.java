package by.losik.errorfreetext.controller;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Tag(name = "Tasks", description = "API для управления задачами коррекции текста")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Создать задачу коррекции",
            description = "Создает новую задачу для коррекции текста через Яндекс.Спеллер"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Задача создана"),
            @ApiResponse(responseCode = "40002", description = "Невалидные данные"),
            @ApiResponse(responseCode = "40501", description = "Неразрешенный метод"),
            @ApiResponse(responseCode = "50001", description = "Серверная ошибка"),
    })
    @PostMapping
    public ResponseEntity<TaskDto.CreateResponse> createTask(
            @Valid @RequestBody TaskDto.CreateRequest request) {

        TaskDto.CreateResponse response = taskService.createTask(request);
        log.info("Task created: {}", response.getTaskId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Получить результат коррекции",
            description = "Возвращает ответ корректировки Яндекс.Спеллера и преобразования локально"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача найдена"),
            @ApiResponse(responseCode = "40401", description = "Задача не найдена"),
            @ApiResponse(responseCode = "40501", description = "Неразрешенный метод"),
            @ApiResponse(responseCode = "50001", description = "Серверная ошибка"),
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto.GetResponse> getTask(@PathVariable UUID taskId) {
        TaskDto.GetResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(response);
    }
}