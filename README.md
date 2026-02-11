# Error Free Text

Веб-приложение для автоматической корректировки текста с использованием API Яндекс.Спеллер.

## Основной стек технологий

- Java 17
- Spring Boot 3.2.3
- Gradle
- PostgreSQL
- Flyway
- OpenAPI
- Docker / docker-compose
- Yandex Speller API
- Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- Caffeine Cache
- MapStruct
- JUnit, Mockito
- Testcontainers, MockWebServer

### Текстовая схема работы:

1. Пользователь отправляет текст, создаётся задача (статус NEW).
2. Scheduler периодически забирает NEW задачи.
3. Текст разбивается на чанки (<=10000 символов).
4. Определяются опции IGNORE_DIGITS / IGNORE_URLS.
5. Yandex Speller API получает сообщение и исправряет нужные позиции.
6. Статус задач изменяется в зависимости от успешности прохождения графа состояний (FAILED/COMPLETED).
7. Пользователь получает исправленный текст, если обратится к соответствующему эндпоинту.

## API Endpoints

#### `POST /api/tasks` - создать задачу
#### `GET /api/tasks/{taskId}` - получить состояние задачи

Если хотите, то я оставил файлик .http, можно повыполнять запросы.
