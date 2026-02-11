CREATE INDEX idx_correction_tasks_status ON correction_tasks(status);
CREATE INDEX idx_correction_tasks_created_at ON correction_tasks(created_at);
CREATE INDEX idx_correction_tasks_language ON correction_tasks(language);
CREATE INDEX idx_correction_tasks_status_created_at ON correction_tasks(status, created_at);