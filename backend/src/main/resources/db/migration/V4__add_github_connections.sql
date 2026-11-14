CREATE TABLE github_connections (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    github_user_id BIGINT NOT NULL,
    account_login VARCHAR(100) NOT NULL,
    encrypted_token TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_github_connection_name CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_github_account_login CHECK (LENGTH(TRIM(account_login)) > 0),
    CONSTRAINT ck_github_encrypted_token CHECK (LENGTH(TRIM(encrypted_token)) > 0)
);

ALTER TABLE project_profiles
    ADD COLUMN github_connection_id UUID;

ALTER TABLE project_profiles
    ADD CONSTRAINT fk_project_github_connection
        FOREIGN KEY (github_connection_id) REFERENCES github_connections(id) ON DELETE RESTRICT;

CREATE INDEX idx_project_github_connection ON project_profiles(github_connection_id);
