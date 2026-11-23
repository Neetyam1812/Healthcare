package Healthcare.HC.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Admin Dashboard View.
 * Contains aggregate KPI metrics, recent activity lists, and system health info.
 * Strictly adheres to privacy requirements: zero individual health data, zero password hashes.
 */
public class AdminDashboardView {

    // Range: 7, 30, or 90 days
    private int selectedRange = 30;

    // KPI Metrics
    private long totalUsers;
    private String totalUsersChange = "+0%";
    private boolean totalUsersTrendUp = true;
    private long newUsersPeriod;
    private long activeUsers;
    private long suspendedUsers;
    private long openTickets;
    private long sosEvents24h;
    private long unacknowledgedSosCount;

    // Role Distribution
    private long familyAdminCount;
    private long familyMemberCount;
    private long caregiverCount;
    private int familyAdminPercent;
    private int familyMemberPercent;
    private int caregiverPercent;

    // Daily Signups for Chart
    private List<DailySignupDto> dailySignups = new ArrayList<>();
    private String dailySignupsJson = "[]";

    // Recent Lists
    private List<SosEventItemDto> recentSosEvents = new ArrayList<>();
    private List<RecentUserDto> recentUsers = new ArrayList<>();
    private List<TicketSummaryDto> recentTickets = new ArrayList<>();
    private List<AuditLogItemDto> recentAuditLogs = new ArrayList<>();

    // System Health
    private boolean systemHealthy = true;
    private long dbPingMs;
    private String jvmUptime = "0m";
    private String appVersion = "1.0.0";
    private String systemStatusLabel = "System healthy";

    public AdminDashboardView() {
    }

    // Nested DTO for daily chart
    public static class DailySignupDto {
        private String date;
        private long count;

        public DailySignupDto(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public long getCount() { return count; }
    }

    // Nested DTO for SOS events
    public static class SosEventItemDto {
        private Long id;
        private String userName;
        private String userEmail;
        private String status;
        private Double latitude;
        private Double longitude;
        private String formattedTime;
        private boolean unacknowledged;

        public SosEventItemDto(Long id, String userName, String userEmail, String status,
                               Double latitude, Double longitude, String formattedTime, boolean unacknowledged) {
            this.id = id;
            this.userName = userName;
            this.userEmail = userEmail;
            this.status = status;
            this.latitude = latitude;
            this.longitude = longitude;
            this.formattedTime = formattedTime;
            this.unacknowledged = unacknowledged;
        }

        public Long getId() { return id; }
        public String getUserName() { return userName; }
        public String getUserEmail() { return userEmail; }
        public String getStatus() { return status; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public String getFormattedTime() { return formattedTime; }
        public boolean isUnacknowledged() { return unacknowledged; }
    }

    // Nested DTO for recent user registrations
    public static class RecentUserDto {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String status;
        private String createdAtFormatted;
        private String initials;

        public RecentUserDto(Long id, String fullName, String email, String role,
                             String status, String createdAtFormatted, String initials) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.status = status;
            this.createdAtFormatted = createdAtFormatted;
            this.initials = initials;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getCreatedAtFormatted() { return createdAtFormatted; }
        public String getInitials() { return initials; }
    }

    // Nested DTO for tickets
    public static class TicketSummaryDto {
        private Long id;
        private String subject;
        private String category;
        private String priority;
        private String status;
        private String createdAtFormatted;

        public TicketSummaryDto(Long id, String subject, String category,
                                String priority, String status, String createdAtFormatted) {
            this.id = id;
            this.subject = subject;
            this.category = category;
            this.priority = priority;
            this.status = status;
            this.createdAtFormatted = createdAtFormatted;
        }

        public Long getId() { return id; }
        public String getSubject() { return subject; }
        public String getCategory() { return category; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public String getCreatedAtFormatted() { return createdAtFormatted; }
    }

    // Nested DTO for audit log items
    public static class AuditLogItemDto {
        private Long id;
        private String actorEmail;
        private String action;
        private String targetType;
        private Long targetId;
        private String detail;
        private String ip;
        private String createdAtFormatted;

        public AuditLogItemDto(Long id, String actorEmail, String action, String targetType,
                               Long targetId, String detail, String ip, String createdAtFormatted) {
            this.id = id;
            this.actorEmail = actorEmail;
            this.action = action;
            this.targetType = targetType;
            this.targetId = targetId;
            this.detail = detail;
            this.ip = ip;
            this.createdAtFormatted = createdAtFormatted;
        }

        public Long getId() { return id; }
        public String getActorEmail() { return actorEmail; }
        public String getAction() { return action; }
        public String getTargetType() { return targetType; }
        public Long getTargetId() { return targetId; }
        public String getDetail() { return detail; }
        public String getIp() { return ip; }
        public String getCreatedAtFormatted() { return createdAtFormatted; }
    }

    // Getters and Setters
    public int getSelectedRange() { return selectedRange; }
    public void setSelectedRange(int selectedRange) { this.selectedRange = selectedRange; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public String getTotalUsersChange() { return totalUsersChange; }
    public void setTotalUsersChange(String totalUsersChange) { this.totalUsersChange = totalUsersChange; }

    public boolean isTotalUsersTrendUp() { return totalUsersTrendUp; }
    public void setTotalUsersTrendUp(boolean totalUsersTrendUp) { this.totalUsersTrendUp = totalUsersTrendUp; }

    public long getNewUsersPeriod() { return newUsersPeriod; }
    public void setNewUsersPeriod(long newUsersPeriod) { this.newUsersPeriod = newUsersPeriod; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getSuspendedUsers() { return suspendedUsers; }
    public void setSuspendedUsers(long suspendedUsers) { this.suspendedUsers = suspendedUsers; }

    public long getOpenTickets() { return openTickets; }
    public void setOpenTickets(long openTickets) { this.openTickets = openTickets; }

    public long getSosEvents24h() { return sosEvents24h; }
    public void setSosEvents24h(long sosEvents24h) { this.sosEvents24h = sosEvents24h; }

    public long getUnacknowledgedSosCount() { return unacknowledgedSosCount; }
    public void setUnacknowledgedSosCount(long unacknowledgedSosCount) { this.unacknowledgedSosCount = unacknowledgedSosCount; }

    public long getFamilyAdminCount() { return familyAdminCount; }
    public void setFamilyAdminCount(long familyAdminCount) { this.familyAdminCount = familyAdminCount; }

    public long getFamilyMemberCount() { return familyMemberCount; }
    public void setFamilyMemberCount(long familyMemberCount) { this.familyMemberCount = familyMemberCount; }

    public long getCaregiverCount() { return caregiverCount; }
    public void setCaregiverCount(long caregiverCount) { this.caregiverCount = caregiverCount; }

    public int getFamilyAdminPercent() { return familyAdminPercent; }
    public void setFamilyAdminPercent(int familyAdminPercent) { this.familyAdminPercent = familyAdminPercent; }

    public int getFamilyMemberPercent() { return familyMemberPercent; }
    public void setFamilyMemberPercent(int familyMemberPercent) { this.familyMemberPercent = familyMemberPercent; }

    public int getCaregiverPercent() { return caregiverPercent; }
    public void setCaregiverPercent(int caregiverPercent) { this.caregiverPercent = caregiverPercent; }

    public List<DailySignupDto> getDailySignups() { return dailySignups; }
    public void setDailySignups(List<DailySignupDto> dailySignups) { this.dailySignups = dailySignups; }

    public String getDailySignupsJson() { return dailySignupsJson; }
    public void setDailySignupsJson(String dailySignupsJson) { this.dailySignupsJson = dailySignupsJson; }

    public List<SosEventItemDto> getRecentSosEvents() { return recentSosEvents; }
    public void setRecentSosEvents(List<SosEventItemDto> recentSosEvents) { this.recentSosEvents = recentSosEvents; }

    public List<RecentUserDto> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<RecentUserDto> recentUsers) { this.recentUsers = recentUsers; }

    public List<TicketSummaryDto> getRecentTickets() { return recentTickets; }
    public void setRecentTickets(List<TicketSummaryDto> recentTickets) { this.recentTickets = recentTickets; }

    public List<AuditLogItemDto> getRecentAuditLogs() { return recentAuditLogs; }
    public void setRecentAuditLogs(List<AuditLogItemDto> recentAuditLogs) { this.recentAuditLogs = recentAuditLogs; }

    public boolean isSystemHealthy() { return systemHealthy; }
    public void setSystemHealthy(boolean systemHealthy) { this.systemHealthy = systemHealthy; }

    public long getDbPingMs() { return dbPingMs; }
    public void setDbPingMs(long dbPingMs) { this.dbPingMs = dbPingMs; }

    public String getJvmUptime() { return jvmUptime; }
    public void setJvmUptime(String jvmUptime) { this.jvmUptime = jvmUptime; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getSystemStatusLabel() { return systemStatusLabel; }
    public void setSystemStatusLabel(String systemStatusLabel) { this.systemStatusLabel = systemStatusLabel; }
}
