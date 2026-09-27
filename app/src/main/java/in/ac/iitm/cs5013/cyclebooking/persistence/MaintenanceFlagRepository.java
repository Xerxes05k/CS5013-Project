package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceFlagRepository extends JpaRepository<MaintenanceFlag, Long> {

    List<MaintenanceFlag> findByResolvedAtIsNullOrderByCreatedAtDesc();

    List<MaintenanceFlag> findByCycleId(Long cycleId);
}
