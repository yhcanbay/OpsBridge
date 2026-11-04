package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrityTest {

    private static final UUID REFERENCE_ID =
            UUID.fromString("b946c162-581a-46ec-a961-066d69c974f1");

    @Autowired private JdbcTemplate jdbc;

    @Test
    void databaseRejectsBlankProjectNameEvenWithoutApiValidation() {
        assertThatThrownBy(() ->
                        jdbc.update(
                                "UPDATE project_profiles SET name = ? WHERE id = ?",
                                "   ",
                                REFERENCE_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        String unchangedName =
                jdbc.queryForObject(
                        "SELECT name FROM project_profiles WHERE id = ?",
                        String.class,
                        REFERENCE_ID);
        assertThat(unchangedName).isEqualTo("Personal_Task_App");
    }

    @Test
    void databaseRejectsUnknownCapabilityEvenWithoutJavaEnum() {
        assertThatThrownBy(() ->
                        jdbc.update(
                                "INSERT INTO project_capabilities (project_id, capability) VALUES"
                                        + " (?, ?)",
                                REFERENCE_ID,
                                "UNKNOWN_CAPABILITY"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void capabilityCannotReferenceAnUnknownProject() {
        assertThatThrownBy(() ->
                        jdbc.update(
                                "INSERT INTO project_capabilities (project_id, capability) VALUES"
                                        + " (?, ?)",
                                UUID.randomUUID(),
                                "BUILD_VIEW"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
