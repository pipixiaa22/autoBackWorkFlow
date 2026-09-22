CREATE INDEX idx_ads_analysis_run_project_created
    ON ads_analysis_run (project_id, created_at, id);

CREATE INDEX idx_ads_generation_task_project_created
    ON ads_generation_task (project_id, created_at, id);

CREATE INDEX idx_ads_export_record_project_created
    ON ads_export_record (project_id, created_at, id);

CREATE INDEX idx_ads_project_trash_created
    ON ads_project (deleted, deleted_at, id);
