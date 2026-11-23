package Healthcare.HC.Repository;

import Healthcare.HC.Entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    long countByStatus(String status);

    List<SupportTicket> findTop5ByStatusOrderByCreatedAtDesc(String status);

    List<SupportTicket> findTop5ByOrderByCreatedAtDesc();

    long countByUserId(Long userId);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
}
