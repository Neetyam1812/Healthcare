package Healthcare.HC.Repository;

import Healthcare.HC.Entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdAndDateTimeAfterOrderByDateTimeAsc(Long userId, LocalDateTime dateTime);

    List<Appointment> findByUserIdAndFamilyMemberIdAndDateTimeAfterOrderByDateTimeAsc(Long userId, Long familyMemberId, LocalDateTime dateTime);

    List<Appointment> findByUserIdAndDateTimeBetweenOrderByDateTimeAsc(Long userId, LocalDateTime start, LocalDateTime end);

    Optional<Appointment> findByIdAndUserId(Long id, Long userId);

    List<Appointment> findTop8ByUserIdOrderByDateTimeDesc(Long userId);
}
