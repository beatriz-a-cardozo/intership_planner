package com.interchangeplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway aplica as migrations no PostgreSQL real e o Hibernate valida o mapeamento (ddl-auto=validate). */
class MigrationsIT extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aplicaTodasAsMigrationsComSucesso() {
        List<String> failed = jdbc.queryForList(
                "select version from flyway_schema_history where success = false", String.class);
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(failed).isEmpty();
        assertThat(applied).isPositive();
    }

    @Test
    void semeiaMoedasSuportadas() throws Exception {
        mvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BRL"))
                .andExpect(jsonPath("$[0].symbol").value("R$"))
                .andExpect(jsonPath("$[1].code").value("EUR"));
    }

    @Test
    void bancoRecusaEmailNaoNormalizado() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into app_user (id, email, password_hash, display_name, created_at, updated_at)
                values (gen_random_uuid(), ' Bia@Example.com', 'x', 'Bia', now(), now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoExigeSenhaParaContaLocal() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into app_user (id, email, display_name, created_at, updated_at)
                values (gen_random_uuid(), 'semsenha@example.com', 'Bia', now(), now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
