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

/** A maintenance issue raised against a cycle, by a guard's note or from a comment. */
@Entity
@Table(name = "maintenance_flags")
public class MaintenanceFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    /** Null when the flag came from a guard rather than a resident's comment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    /** The issue label, e.g. "chain", "brakes", "tyre". */
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagSource source;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected MaintenanceFlag() {
    }

    public MaintenanceFlag(Cycle cycle, Comment comment, String label, FlagSource source, Instant createdAt) {
        this.cycle = cycle;
        this.comment = comment;
        this.label = label;
        this.source = source;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public Comment getComment() {
        return comment;
    }

    public String getLabel() {
        return label;
    }

    public FlagSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
