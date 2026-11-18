ALTER TABLE project_profiles
    ADD COLUMN tracked_branch VARCHAR(255) NOT NULL DEFAULT 'main';

ALTER TABLE project_profiles
    ADD COLUMN deployment_info_url VARCHAR(500);

ALTER TABLE project_profiles
    ADD CONSTRAINT ck_project_tracked_branch_not_blank CHECK (LENGTH(TRIM(tracked_branch)) > 0);
