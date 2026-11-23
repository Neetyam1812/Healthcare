package Healthcare.HC.Repository;

import Healthcare.HC.Entity.VitalReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {

    List<VitalReading> findByUserIdOrderByRecordedAtDesc(Long userId);

    List<VitalReading> findByUserIdAndFamilyMemberIdOrderByRecordedAtDesc(Long userId, Long familyMemberId);

    List<VitalReading> findByUserIdAndTypeOrderByRecordedAtDesc(Long userId, String type);

    List<VitalReading> findByUserIdAndFamilyMemberIdAndTypeOrderByRecordedAtDesc(Long userId, Long familyMemberId, String type);

    Optional<VitalReading> findFirstByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<VitalReading> findFirstByUserIdAndFamilyMemberIdOrderByRecordedAtDesc(Long userId, Long familyMemberId);

    List<VitalReading> findTop8ByUserIdOrderByRecordedAtDesc(Long userId);
}
