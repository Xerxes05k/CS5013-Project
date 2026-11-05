package in.ac.iitm.cs5013.cyclebooking.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;

/** cycle_parts holds one row per (cycle, part), enforced by the database, not only by code. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CyclePartUniqueIndex.class)
class CyclePartRepositoryTest {

    private static final Instant T = Instant.parse("2026-11-05T05:00:00Z");

    @Autowired
    private CyclePartRepository parts;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void partRoundTripsAndIsFoundByCycleAndPart() {
        Cycle cycle = entityManager.persist(new Cycle("PRT-01"));
        parts.save(new CyclePart(cycle, Part.BRAKES, PartCondition.BROKEN, FlagSource.GUARD, T, "front brake loose"));
        entityManager.flush();
        entityManager.clear();

        CyclePart found = parts.findByCycleIdAndPart(cycle.getId(), Part.BRAKES).orElseThrow();
        assertThat(found.getCondition()).isEqualTo(PartCondition.BROKEN);
        assertThat(found.getChangedBy()).isEqualTo(FlagSource.GUARD);
        assertThat(found.getNote()).isEqualTo("front brake loose");
        assertThat(parts.findByCycleIdAndPart(cycle.getId(), Part.BELL)).isEmpty();
    }

    @Test
    void secondRowForTheSamePartOfTheSameCycleIsRejected() {
        Cycle cycle = entityManager.persist(new Cycle("PRT-02"));
        parts.save(new CyclePart(cycle, Part.TYRE, PartCondition.BROKEN, FlagSource.GUARD, T, null));
        entityManager.flush();

        assertThatThrownBy(() -> {
            parts.save(new CyclePart(cycle, Part.TYRE, PartCondition.OK, FlagSource.GS, T, null));
            entityManager.flush();
        // Spring maps this SQLite error to a plain DataAccessException, so check the cause.
        }).isInstanceOf(DataAccessException.class)
                .hasMessageContaining("UNIQUE constraint failed: cycle_parts.cycle_id, cycle_parts.part");
    }
}
