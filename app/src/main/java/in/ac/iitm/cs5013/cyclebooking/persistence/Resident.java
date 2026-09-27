package in.ac.iitm.cs5013.cyclebooking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A hostel resident who may book cycles.
 *
 * Note: no ID card photo is ever stored here. Only the text OCR extracts from the
 * card (name, roll number, hostel) is compared against these fields — see ADR 0003.
 */
@Entity
@Table(name = "residents")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rollNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String hostel;

    @Column(nullable = false, unique = true)
    private String smailAddress;

    @Column(nullable = false)
    private String passcodeHash;

    protected Resident() {
    }

    public Resident(String rollNumber, String name, String hostel, String smailAddress, String passcodeHash) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.hostel = hostel;
        this.smailAddress = smailAddress;
        this.passcodeHash = passcodeHash;
    }

    public Long getId() {
        return id;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getName() {
        return name;
    }

    public String getHostel() {
        return hostel;
    }

    public String getSmailAddress() {
        return smailAddress;
    }

    public String getPasscodeHash() {
        return passcodeHash;
    }

    public void setPasscodeHash(String passcodeHash) {
        this.passcodeHash = passcodeHash;
    }
}
