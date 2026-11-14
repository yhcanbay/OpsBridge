package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GitHubMigrationTest {

    @Test
    void upgradesVersionThreeWithoutChangingProjectsAndEnforcesConnectionReferences() {
        String url = "jdbc:h2:mem:github-upgrade-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID projectId = jdbc.queryForObject("SELECT id FROM project_profiles WHERE name = 'Personal_Task_App'", UUID.class);

        Flyway.configure().dataSource(dataSource).load().migrate();
        assertThat(jdbc.queryForObject("SELECT name FROM project_profiles WHERE id = ?", String.class, projectId))
                .isEqualTo("Personal_Task_App");
        assertThat(jdbc.queryForObject("SELECT github_connection_id FROM project_profiles WHERE id = ?", UUID.class, projectId))
                .isNull();
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
