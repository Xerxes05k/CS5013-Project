package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByBookingId(Long bookingId);
}
