package in.ac.iitm.cs5013.cyclebooking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The current condition of one part of one cycle (ADR 0005). The history of who said
 * what lives in maintenance_flags; this row is only the latest word.
 *
 * A cycle has no row for a part until that part's condition first changes, and a
 * missing row means OK. So a cycle added with one INSERT (docs/DEPLOY.md) is fine.
 */
@Entity
// One row per (cycle, part) is enforced by CyclePartUniqueIndex, not an annotation: the
// SQLite dialect silently drops multi-column unique constraints and indexes.
@Table(name = "cycle_parts")
public class CyclePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", nullable = false)
    private PartCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagSource changedBy;

    @Column(nullable = false)
    private Instant updatedAt;

    /** The guard's or GS's words with the last change, if any. */
    @Column(length = 500)
    private String note;

    protected CyclePart() {
    }

    public CyclePart(Cycle cycle, Part part, PartCondition condition, FlagSource changedBy, Instant updatedAt,
            String note) {
        this.cycle = cycle;
        this.part = part;
        change(condition, changedBy, updatedAt, note);
    }

    /** Records the latest word on this part. The previous one survives as a flag. */
    public void change(PartCondition condition, FlagSource changedBy, Instant updatedAt, String note) {
        this.condition = condition;
        this.changedBy = changedBy;
        this.updatedAt = updatedAt;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public Part getPart() {
        return part;
    }

    public PartCondition getCondition() {
        return condition;
    }

    public FlagSource getChangedBy() {
        return changedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getNote() {
        return note;
    }
}
