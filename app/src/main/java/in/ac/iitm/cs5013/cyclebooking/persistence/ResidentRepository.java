package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    Optional<Resident> findByRollNumberIgnoreCase(String rollNumber);

    Optional<Resident> findBySmailAddressIgnoreCase(String smailAddress);
}
