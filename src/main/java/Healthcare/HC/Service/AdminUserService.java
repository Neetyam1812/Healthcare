package Healthcare.HC.Service;

import Healthcare.HC.Entity.AuditLog;
import Healthcare.HC.Entity.SupportTicket;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.AuditLogRepository;
import Healthcare.HC.Repository.EmergencyEventRepository;
import Healthcare.HC.Repository.FamilyMemberRepository;
import Healthcare.HC.Repository.SupportTicketRepository;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.dto.AdminDashboardView;
import Healthcare.HC.dto.AdminUserDetail;
import Healthcare.HC.dto.AdminUserRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final SessionRegistry sessionRegistry;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    public AdminUserService(UserRepository userRepository,
                            FamilyMemberRepository familyMemberRepository,
                            SupportTicketRepository supportTicketRepository,
                            EmergencyEventRepository emergencyEventRepository,
                            AuditLogRepository auditLogRepository,
                            AuditService auditService,
                            SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.emergencyEventRepository = emergencyEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.sessionRegistry = sessionRegistry;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    @Transactional(readOnly = true)
    public Page<AdminUserRow> searchUsers(String query, String status, String sort, int page, int size, Long currentUserId) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("name".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "fullName");
        } else if ("last-login".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "lastLoginAt");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        String normalizedStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) ? null : status.trim();
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();

        Page<User> usersPage = userRepository.searchUsers(normalizedQuery, normalizedStatus, pageable);

        return usersPage.map(u -> {
            String createdStr = u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "—";
            String loginStr = u.getLastLoginAt() != null ? u.getLastLoginAt().format(DATE_FMT) : "Never";
            boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(u.getPlatformRole());
            boolean isSelf = (currentUserId != null && currentUserId.equals(u.getId()));

            return new AdminUserRow(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getRole(),
                    u.getPlatformRole(),
                    u.getStatus(),
                    createdStr,
                    loginStr,
                    getInitials(u.getFullName()),
                    isSuperAdmin,
                    isSelf
            );
        });
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    @Transactional(readOnly = true)
    public AdminUserDetail getUserDetail(Long targetUserId, Long currentUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + targetUserId));

        AdminUserDetail detail = new AdminUserDetail();
        detail.setId(user.getId());
        detail.setFullName(user.getFullName());
        detail.setEmail(user.getEmail());
        detail.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "—");
        detail.setRole(user.getRole());
        detail.setPlatformRole(user.getPlatformRole());
        detail.setStatus(user.getStatus());
        detail.setEnabled(user.isEnabled());
        detail.setCreatedAtFormatted(user.getCreatedAt() != null ? user.getCreatedAt().format(TIME_FMT) : "—");
        detail.setLastLoginFormatted(user.getLastLoginAt() != null ? user.getLastLoginAt().format(TIME_FMT) : "Never");
        detail.setInitials(getInitials(user.getFullName()));

        // Check safety rules
        detail.setSuperAdmin("SUPER_ADMIN".equalsIgnoreCase(user.getPlatformRole()));
        detail.setSelf(currentUserId != null && currentUserId.equals(user.getId()));

        // Aggregates only - ZERO personal health data
        detail.setFamilyMemberCount(familyMemberRepository.countByUserId(targetUserId));
        detail.setSupportTicketCount(supportTicketRepository.countByUserId(targetUserId));
        detail.setSosEventCount(emergencyEventRepository.countByUserId(targetUserId));

        // Audit log for this target
        List<AuditLog> logs = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("USER", targetUserId);
        List<AdminDashboardView.AuditLogItemDto> logDtos = new ArrayList<>();
        for (AuditLog l : logs) {
            String timeStr = l.getCreatedAt() != null ? l.getCreatedAt().format(TIME_FMT) : "—";
            logDtos.add(new AdminDashboardView.AuditLogItemDto(
                    l.getId(), l.getActorEmail(), l.getAction(), l.getTargetType(),
                    l.getTargetId(), l.getDetail(), l.getIp(), timeStr
            ));
        }
        detail.setAuditLogs(logDtos);

        // Support tickets for this user
        List<SupportTicket> tickets = supportTicketRepository.findByUserIdOrderByCreatedAtDesc(targetUserId);
        List<AdminDashboardView.TicketSummaryDto> ticketDtos = new ArrayList<>();
        for (SupportTicket t : tickets) {
            String timeStr = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "—";
            ticketDtos.add(new AdminDashboardView.TicketSummaryDto(
                    t.getId(), t.getSubject(), t.getCategory(), t.getPriority(), t.getStatus(), timeStr
            ));
        }
        detail.setSupportTickets(ticketDtos);

        return detail;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT')")
    @Transactional
    public void changeUserStatus(Long targetUserId, String action, String reason,
                                 Long currentUserId, String currentEmail, String ip) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A valid reason of at least 5 characters is required.");
        }

        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new IllegalStateException("Administrators cannot suspend or modify their own account status.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + targetUserId));

        if ("SUPER_ADMIN".equalsIgnoreCase(user.getPlatformRole())) {
            throw new IllegalStateException("SUPER_ADMIN accounts cannot be suspended.");
        }

        String trimmedReason = reason.trim();

        if ("SUSPEND".equalsIgnoreCase(action)) {
            user.setStatus("SUSPENDED");
            user.setEnabled(false);
            userRepository.save(user);

            // Invalidate active sessions
            invalidateUserSessions(targetUserId);

            // Audit
            auditService.log(currentUserId, currentEmail, "USER_SUSPENDED", "USER", targetUserId,
                    "Suspended. Reason: " + trimmedReason, ip);
        } else if ("ACTIVATE".equalsIgnoreCase(action)) {
            user.setStatus("ACTIVE");
            user.setEnabled(true);
            userRepository.save(user);

            // Audit
            auditService.log(currentUserId, currentEmail, "USER_ACTIVATED", "USER", targetUserId,
                    "Activated. Reason: " + trimmedReason, ip);
        } else {
            throw new IllegalArgumentException("Invalid status action: " + action);
        }
    }

    private void invalidateUserSessions(Long userId) {
        try {
            for (Object principal : sessionRegistry.getAllPrincipals()) {
                if (principal instanceof AppUserDetails appUser) {
                    if (appUser.getId().equals(userId)) {
                        for (SessionInformation sessionInfo : sessionRegistry.getAllSessions(principal, false)) {
                            sessionInfo.expireNow();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Logged or handled gracefully
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "FC";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
