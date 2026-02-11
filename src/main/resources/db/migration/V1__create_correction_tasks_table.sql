CREATE TABLE correction_tasks (
    id UUID PRIMARY KEY,
    original_text TEXT NOT NULL,
    corrected_text TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'PROCESSING', 'COMPLETED', 'FAILED')),
    language VARCHAR(2) NOT NULL CHECK (language IN ('RU', 'EN')),
    options INTEGER,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

COMMENT ON TABLE correction_tasks IS 'Таблица задач автоматической корректировки текста';
COMMENT ON COLUMN correction_tasks.id IS 'Уникальный идентификатор задачи';
COMMENT ON COLUMN correction_tasks.original_text IS 'Исходный текст для коррекции';
COMMENT ON COLUMN correction_tasks.corrected_text IS 'Скорректированный текст';
COMMENT ON COLUMN correction_tasks.status IS 'Статус задачи: NEW, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN correction_tasks.language IS 'Язык текста: RU или EN';
COMMENT ON COLUMN correction_tasks.options IS 'Опции Яндекс Спеллера (битовая маска)';
COMMENT ON COLUMN correction_tasks.error_message IS 'Сообщение об ошибке при обработке';
COMMENT ON COLUMN correction_tasks.created_at IS 'Время создания задачи';
COMMENT ON COLUMN correction_tasks.processed_at IS 'Время обработки задачи';
COMMENT ON COLUMN correction_tasks.version IS 'Версия для оптимистичной блокировки';