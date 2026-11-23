package Healthcare.HC.Repository;

import Healthcare.HC.Entity.MedicineLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicineLogRepository extends JpaRepository<MedicineLog, Long> {

    List<MedicineLog> findByMedicineIdAndTakenAtBetween(Long medicineId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT ml FROM MedicineLog ml WHERE ml.medicine.user.id = :userId AND ml.takenAt BETWEEN :start AND :end")
    List<MedicineLog> findByUserIdAndTakenAtBetween(@Param("userId") Long userId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(ml) > 0 FROM MedicineLog ml WHERE ml.medicine.id = :medicineId AND ml.takenAt BETWEEN :start AND :end")
    boolean existsByMedicineIdAndTakenAtBetween(@Param("medicineId") Long medicineId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT ml FROM MedicineLog ml WHERE ml.medicine.user.id = :userId ORDER BY ml.takenAt DESC")
    List<MedicineLog> findTop8ByUserIdOrderByTakenAtDesc(@Param("userId") Long userId);
}
