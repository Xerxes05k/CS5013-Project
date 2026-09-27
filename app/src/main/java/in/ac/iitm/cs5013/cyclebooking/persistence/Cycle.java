package in.ac.iitm.cs5013.cyclebooking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A physical cycle in the hostel pool. */
@Entity
@Table(name = "cycles")
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What is painted/stickered on the cycle, e.g. "JAM-04". */
    @Column(nullable = false, unique = true)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleState state = CycleState.AVAILABLE;

    private Instant lastReturnedAt;

    /**
     * Optimistic lock. Two residents booking the same cycle in the same instant is the
     * concurrency case from the design doc: the second commit fails rather than
     * silently overwriting the first.
     */
    @Version
    private Long version;

    protected Cycle() {
    }

    public Cycle(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public CycleState getState() {
        return state;
    }

    public void setState(CycleState state) {
        this.state = state;
    }

    public Instant getLastReturnedAt() {
        return lastReturnedAt;
    }

    public void setLastReturnedAt(Instant lastReturnedAt) {
        this.lastReturnedAt = lastReturnedAt;
    }

    public Long getVersion() {
        return version;
    }
}
