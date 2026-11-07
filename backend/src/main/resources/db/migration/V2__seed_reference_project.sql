-- İlk örnek yalnızca veridir; servislerde proje adına özel davranış bulunmaz.
-- Job yolu kullanıcı ortamında doğrulanmadığı için boş bırakılır.
INSERT INTO project_profiles (
    id, name, repository_url, environment, jenkins_base_url_ref,
    jenkins_job_path, container_label_selector, health_url, enabled, created_at, updated_at
) VALUES (
    'b946c162-581a-46ec-a961-066d69c974f1',
    'Personal_Task_App', 'https://github.com/yhcanbay/Personal_Task_App', 'local',
    'local-jenkins', NULL, 'com.docker.compose.project=personal_task_app',
    'http://localhost:8080/api/settings', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO project_capabilities (project_id, capability) VALUES
('b946c162-581a-46ec-a961-066d69c974f1', 'BUILD_VIEW'),
('b946c162-581a-46ec-a961-066d69c974f1', 'BUILD_TRIGGER'),
('b946c162-581a-46ec-a961-066d69c974f1', 'CONTAINER_VIEW'),
('b946c162-581a-46ec-a961-066d69c974f1', 'HEALTH_VIEW');
