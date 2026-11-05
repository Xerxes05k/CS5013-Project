package in.ac.iitm.cs5013.cyclebooking.persistence;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Makes the database itself refuse a second cycle_parts row for the same part of the
 * same cycle. Hibernate's SQLite dialect drops a multi-column @UniqueConstraint or
 * unique @Index without a warning (it creates neither), so the index is created here.
 *
 * Taking the EntityManagerFactory makes Spring build it first, so Hibernate has created
 * the table before this runs. IF NOT EXISTS makes it safe on every start, in every
 * profile, including prod where data.sql does not run.
 */
@Component
class CyclePartUniqueIndex {

    private final JdbcTemplate jdbc;

    CyclePartUniqueIndex(JdbcTemplate jdbc, EntityManagerFactory tablesAlreadyCreated) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void create() {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_cycle_parts_cycle_part ON cycle_parts (cycle_id, part)");
    }
}
