package Healthcare.HC.Repository;

import Healthcare.HC.Entity.EmergencyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {

    List<EmergencyEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<EmergencyEvent> findByIdAndUserId(Long id, Long userId);

    List<EmergencyEvent> findTop5ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByStatus(String status);

    long countByStatusNot(String status);

    long countByUserId(Long userId);
}
