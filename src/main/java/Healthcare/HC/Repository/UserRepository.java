package Healthcare.HC.Repository;

import Healthcare.HC.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPlatformRoleIsNotNull();

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByLastLoginAtAfter(LocalDateTime dateTime);

    long countByStatus(String status);

    long countByRole(String role);

    List<User> findTop8ByOrderByCreatedAtDesc();

    Page<User> findAll(Pageable pageable);

    Page<User> findByStatus(String status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query,
                           @Param("status") String status,
                           Pageable pageable);
}
