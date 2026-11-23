package Healthcare.HC.Repository;

import Healthcare.HC.Entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findByUserIdAndActiveTrue(Long userId);

    List<Medicine> findByUserIdAndFamilyMemberIdAndActiveTrue(Long userId, Long familyMemberId);

    Optional<Medicine> findByIdAndUserId(Long id, Long userId);

    List<Medicine> findByUserIdAndActiveTrueAndStockCountLessThanEqual(Long userId, Integer threshold);
}
