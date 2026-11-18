package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GitHubMigrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"3", "5"})
    void upgradesExistingSchemaWithoutChangingProjectsOrAppliedHistory(String initialVersion) {
        String url = "jdbc:h2:mem:github-upgrade-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        Flyway.configure().dataSource(dataSource).target(initialVersion).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID projectId = jdbc.queryForObject("SELECT id FROM project_profiles WHERE name = 'Personal_Task_App'", UUID.class);

        if ("5".equals(initialVersion)) {
            jdbc.update("UPDATE project_profiles SET local_repository_ref = ?, tracked_branch = ?, deployment_info_url = ? WHERE id = ?",
                    "legacy-source", "release/legacy", "http://legacy.test/build-info", projectId);
        }

        Flyway.configure().dataSource(dataSource).load().migrate();
        assertThat(jdbc.queryForObject("SELECT name FROM project_profiles WHERE id = ?", String.class, projectId))
                .isEqualTo("Personal_Task_App");
        assertThat(jdbc.queryForObject("SELECT github_connection_id FROM project_profiles WHERE id = ?", UUID.class, projectId))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT \"checksum\" FROM \"flyway_schema_history\" WHERE \"version\" = '4'", Integer.class))
                .isEqualTo(1389696536);
        assertThat(jdbc.queryForObject("SELECT \"checksum\" FROM \"flyway_schema_history\" WHERE \"version\" = '5'", Integer.class))
                .isEqualTo(1730378085);
        assertThat(jdbc.queryForObject("SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"script\" = 'V6__add_github_connections.sql'", String.class))
                .isEqualTo("6");
        if ("5".equals(initialVersion)) {
            assertThat(jdbc.queryForObject("SELECT tracked_branch FROM project_profiles WHERE id = ?", String.class, projectId))
                    .isEqualTo("release/legacy");
            assertThat(jdbc.queryForObject("SELECT local_repository_ref FROM project_profiles WHERE id = ?", String.class, projectId))
                    .isEqualTo("legacy-source");
            assertThat(jdbc.queryForObject("SELECT deployment_info_url FROM project_profiles WHERE id = ?", String.class, projectId))
                    .isEqualTo("http://legacy.test/build-info");
        }
        assertThatThrownBy(() -> jdbc.update("UPDATE project_profiles SET github_connection_id = ? WHERE id = ?", UUID.randomUUID(), projectId))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID connectionId = UUID.randomUUID();
        jdbc.update("INSERT INTO github_connections (id, name, github_user_id, account_login, encrypted_token, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                connectionId, "Test", 42, "example", "ciphertext-test-only");
        jdbc.update("UPDATE project_profiles SET github_connection_id = ? WHERE id = ?", connectionId, projectId);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM github_connections WHERE id = ?", connectionId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
