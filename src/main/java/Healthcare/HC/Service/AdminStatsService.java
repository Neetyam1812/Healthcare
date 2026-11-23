package Healthcare.HC.Service;

import Healthcare.HC.Entity.AuditLog;
import Healthcare.HC.Entity.EmergencyEvent;
import Healthcare.HC.Entity.SupportTicket;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.AuditLogRepository;
import Healthcare.HC.Repository.EmergencyEventRepository;
import Healthcare.HC.Repository.SupportTicketRepository;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.dto.AdminDashboardView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final DataSource dataSource;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public AdminStatsService(UserRepository userRepository,
                             EmergencyEventRepository emergencyEventRepository,
                             SupportTicketRepository supportTicketRepository,
                             AuditLogRepository auditLogRepository,
                             AuditService auditService,
                             DataSource dataSource) {
        this.userRepository = userRepository;
        this.emergencyEventRepository = emergencyEventRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.dataSource = dataSource;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    @Transactional(readOnly = true)
    public AdminDashboardView getDashboardView(int rangeDays) {
        if (rangeDays != 7 && rangeDays != 30 && rangeDays != 90) {
            rangeDays = 30;
        }

        AdminDashboardView view = new AdminDashboardView();
        view.setSelectedRange(rangeDays);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minusDays(rangeDays);
        LocalDateTime priorPeriodStart = now.minusDays(rangeDays * 2L);

        // 1. Total users and Period Change
        long totalUsers = userRepository.count();
        view.setTotalUsers(totalUsers);

        long currentPeriodUsers = userRepository.countByCreatedAtBetween(periodStart, now);
        long priorPeriodUsers = userRepository.countByCreatedAtBetween(priorPeriodStart, periodStart);
        view.setNewUsersPeriod(currentPeriodUsers);

        if (priorPeriodUsers == 0) {
            view.setTotalUsersChange(currentPeriodUsers > 0 ? "+100%" : "0%");
            view.setTotalUsersTrendUp(true);
        } else {
            long diff = currentPeriodUsers - priorPeriodUsers;
            double percent = ((double) diff / priorPeriodUsers) * 100.0;
            if (percent >= 0) {
                view.setTotalUsersChange(String.format("+%.1f%%", percent));
                view.setTotalUsersTrendUp(true);
            } else {
                view.setTotalUsersChange(String.format("%.1f%%", percent));
                view.setTotalUsersTrendUp(false);
            }
        }

        // 2. Active users (logged in within last 30 days)
        view.setActiveUsers(userRepository.countByLastLoginAtAfter(now.minusDays(30)));

        // 3. Suspended users
        view.setSuspendedUsers(userRepository.countByStatus("SUSPENDED"));

        // 4. Open tickets
        view.setOpenTickets(supportTicketRepository.countByStatus("OPEN"));

        // 5. SOS Events in last 24h & Unacknowledged count
        view.setSosEvents24h(emergencyEventRepository.countByCreatedAtAfter(now.minusHours(24)));
        view.setUnacknowledgedSosCount(emergencyEventRepository.countByStatusNot("ACKNOWLEDGED"));

        // 6. Role Breakdown
        long familyAdminCount = userRepository.countByRole("FAMILY_ADMIN");
        long familyMemberCount = userRepository.countByRole("FAMILY_MEMBER");
        long caregiverCount = userRepository.countByRole("CAREGIVER");
        long roleTotal = familyAdminCount + familyMemberCount + caregiverCount;

        view.setFamilyAdminCount(familyAdminCount);
        view.setFamilyMemberCount(familyMemberCount);
        view.setCaregiverCount(caregiverCount);

        if (roleTotal > 0) {
            view.setFamilyAdminPercent((int) Math.round(((double) familyAdminCount / roleTotal) * 100));
            view.setFamilyMemberPercent((int) Math.round(((double) familyMemberCount / roleTotal) * 100));
            view.setCaregiverPercent((int) Math.round(((double) caregiverCount / roleTotal) * 100));
        } else {
            view.setFamilyAdminPercent(50);
            view.setFamilyMemberPercent(30);
            view.setCaregiverPercent(20);
        }

        // 7. Daily Sign-ups for Chart
        List<AdminDashboardView.DailySignupDto> dailyList = new ArrayList<>();
        StringBuilder jsonBuilder = new StringBuilder("[");
        int chartPoints = Math.min(rangeDays, 14); // up to 14 points for optimal SVG rendering
        int stepDays = Math.max(1, rangeDays / chartPoints);

        for (int i = chartPoints - 1; i >= 0; i--) {
            LocalDate dayDate = LocalDate.now().minusDays((long) i * stepDays);
            LocalDateTime dayStart = dayDate.atStartOfDay();
            LocalDateTime dayEnd = dayDate.plusDays(stepDays).atStartOfDay();

            long count = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            String dateLabel = dayDate.format(DATE_FMT);

            dailyList.add(new AdminDashboardView.DailySignupDto(dateLabel, count));
            if (dailyList.size() > 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append(String.format("{\"date\":\"%s\",\"count\":%d}", dateLabel, count));
        }
        jsonBuilder.append("]");
        view.setDailySignups(dailyList);
        view.setDailySignupsJson(jsonBuilder.toString());

        // 8. Recent SOS events (top 5)
        List<EmergencyEvent> sosEntities = emergencyEventRepository.findTop5ByOrderByCreatedAtDesc();
        List<AdminDashboardView.SosEventItemDto> sosDtos = new ArrayList<>();
        for (EmergencyEvent ev : sosEntities) {
            String userName = ev.getUser() != null ? ev.getUser().getFullName() : "Unknown User";
            String userEmail = ev.getUser() != null ? ev.getUser().getEmail() : "N/A";
            boolean unack = !"ACKNOWLEDGED".equalsIgnoreCase(ev.getStatus()) && !"RESOLVED".equalsIgnoreCase(ev.getStatus());
            String timeStr = ev.getCreatedAt() != null ? ev.getCreatedAt().format(TIME_FMT) : "Recent";
            sosDtos.add(new AdminDashboardView.SosEventItemDto(
                    ev.getId(), userName, userEmail, ev.getStatus(),
                    ev.getLatitude(), ev.getLongitude(), timeStr, unack
            ));
        }
        view.setRecentSosEvents(sosDtos);

        // 9. Recent Registrations (top 8)
        List<User> recentUserEntities = userRepository.findTop8ByOrderByCreatedAtDesc();
        List<AdminDashboardView.RecentUserDto> userDtos = new ArrayList<>();
        for (User u : recentUserEntities) {
            String timeStr = u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "Recent";
            String initials = getInitials(u.getFullName());
            userDtos.add(new AdminDashboardView.RecentUserDto(
                    u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                    u.getStatus(), timeStr, initials
            ));
        }
        view.setRecentUsers(userDtos);

        // 10. Recent Tickets (top 5)
        List<SupportTicket> ticketEntities = supportTicketRepository.findTop5ByOrderByCreatedAtDesc();
        List<AdminDashboardView.TicketSummaryDto> ticketDtos = new ArrayList<>();
        for (SupportTicket t : ticketEntities) {
            String timeStr = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "Recent";
            ticketDtos.add(new AdminDashboardView.TicketSummaryDto(
                    t.getId(), t.getSubject(), t.getCategory(), t.getPriority(), t.getStatus(), timeStr
            ));
        }
        view.setRecentTickets(ticketDtos);

        // 11. Recent Audit Logs (top 8)
        List<AuditLog> auditEntities = auditLogRepository.findTop8ByOrderByCreatedAtDesc();
        List<AdminDashboardView.AuditLogItemDto> auditDtos = new ArrayList<>();
        for (AuditLog a : auditEntities) {
            String timeStr = a.getCreatedAt() != null ? a.getCreatedAt().format(TIME_FMT) : "Recent";
            auditDtos.add(new AdminDashboardView.AuditLogItemDto(
                    a.getId(), a.getActorEmail(), a.getAction(), a.getTargetType(),
                    a.getTargetId(), a.getDetail(), a.getIp(), timeStr
            ));
        }
        view.setRecentAuditLogs(auditDtos);

        // 12. System Health Check
        long dbPing = checkDatabasePing();
        view.setDbPingMs(dbPing);
        view.setSystemHealthy(dbPing >= 0 && dbPing < 500);
        view.setSystemStatusLabel(view.isSystemHealthy() ? "System healthy" : "System degraded");
        view.setJvmUptime(formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
        view.setAppVersion("1.0.0");

        return view;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT')")
    @Transactional
    public void acknowledgeSos(Long eventId, Long actorId, String actorEmail, String ip) {
        EmergencyEvent event = emergencyEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency event not found with ID: " + eventId));
        event.setStatus("ACKNOWLEDGED");
        emergencyEventRepository.save(event);

        auditService.log(actorId, actorEmail, "SOS_ACKNOWLEDGED", "EMERGENCY_EVENT", eventId,
                "SOS alert acknowledged by staff", ip);
    }

    private long checkDatabasePing() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                return System.currentTimeMillis() - start;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else {
            return Math.max(1, minutes) + "m";
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
