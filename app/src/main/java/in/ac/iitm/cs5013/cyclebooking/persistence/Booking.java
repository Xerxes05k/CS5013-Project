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
import java.time.Duration;
import java.time.Instant;

/** One resident's reservation of one cycle, from booking through return. */
@Entity
@Table(name = "bookings")
public class Booking {

    /** The hostel's rule: a cycle may be held for at most 24 hours per rental. */
    public static final Duration MAX_HOLD = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant pickedUpAt;

    private Instant returnedAt;

    /** Null on bookings made before ID checks existed (week 2 and earlier). */
    @Enumerated(EnumType.STRING)
    private IdCheck idCheck;

    protected Booking() {
    }

    public Booking(Resident resident, Cycle cycle, Instant createdAt) {
        this.resident = resident;
        this.cycle = cycle;
        this.createdAt = createdAt;
    }

    public Booking(Resident resident, Cycle cycle, Instant createdAt, IdCheck idCheck) {
        this(resident, cycle, createdAt);
        this.idCheck = idCheck;
    }

    /**
     * True once the cycle has been held longer than {@link #MAX_HOLD} without being
     * returned. Derived rather than stored so it cannot go stale.
     */
    public boolean isOverdue(Instant now) {
        if (pickedUpAt == null || returnedAt != null) {
            return false;
        }
        return Duration.between(pickedUpAt, now).compareTo(MAX_HOLD) > 0;
    }

    public Long getId() {
        return id;
    }

    public Resident getResident() {
        return resident;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public IdCheck getIdCheck() {
        return idCheck;
    }
}
