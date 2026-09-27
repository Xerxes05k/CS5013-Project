package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    List<Cycle> findByStateOrderByLabelAsc(CycleState state);

    List<Cycle> findAllByOrderByLabelAsc();
}
