ALTER TABLE project_profiles
    ADD CONSTRAINT ck_project_name_not_blank CHECK (LENGTH(TRIM(name)) > 0);

ALTER TABLE project_profiles
    ADD CONSTRAINT ck_project_environment_not_blank CHECK (LENGTH(TRIM(environment)) > 0);

ALTER TABLE project_capabilities
    ADD CONSTRAINT ck_project_capability CHECK (
        capability IN ('BUILD_VIEW', 'BUILD_TRIGGER', 'CONTAINER_VIEW', 'HEALTH_VIEW', 'SECURITY_VIEW')
    );
