package Healthcare.HC.Service;

import Healthcare.HC.Entity.AuditLog;
import Healthcare.HC.Repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void log(Long actorId, String actorEmail, String action, String targetType, Long targetId, String detail, String ip) {
        try {
            AuditLog auditLog = new AuditLog(actorId, actorEmail, action, targetType, targetId, detail, ip);
            auditLogRepository.save(auditLog);
            logger.info("AUDIT: action={}, actorEmail={}, targetType={}, targetId={}, ip={}",
                    action, actorEmail, targetType, targetId, ip);
        } catch (Exception e) {
            logger.error("Failed to write audit log entry for action={}: {}", action, e.getMessage());
        }
    }
}
