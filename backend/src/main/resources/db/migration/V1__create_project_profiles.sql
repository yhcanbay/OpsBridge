CREATE TABLE project_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    repository_url VARCHAR(500) NOT NULL,
    environment VARCHAR(30) NOT NULL,
    jenkins_base_url_ref VARCHAR(100),
    jenkins_job_path VARCHAR(200),
    container_label_selector VARCHAR(250),
    health_url VARCHAR(500),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE project_capabilities (
    project_id UUID NOT NULL REFERENCES project_profiles(id) ON DELETE CASCADE,
    capability VARCHAR(30) NOT NULL,
    PRIMARY KEY (project_id, capability)
);
