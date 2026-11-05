package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CyclePartRepository extends JpaRepository<CyclePart, Long> {

    /** Only the parts that have ever changed; any part not listed is OK. */
    List<CyclePart> findByCycleId(Long cycleId);

    Optional<CyclePart> findByCycleIdAndPart(Long cycleId, Part part);
}
