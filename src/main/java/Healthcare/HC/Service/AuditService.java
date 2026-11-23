package Healthcare.HC.Service;

public interface AuditService {
    void log(Long actorId, String actorEmail, String action, String targetType, Long targetId, String detail, String ip);
}
