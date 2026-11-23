package Healthcare.HC.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Admin User Detail page.
 * Contains only account information, member counts, tickets, and audit history.
 * Strictly forbidden to include medicines, vitals, records, allergies, conditions, or password hashes.
 */
public class AdminUserDetail {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String platformRole;
    private String status;
    private boolean enabled;
    private String createdAtFormatted;
    private String lastLoginFormatted;
    private String initials;

    // Aggregate counts only (NO medical/personal detail)
    private long familyMemberCount;
    private long supportTicketCount;
    private long sosEventCount;

    // Safety checks
    private boolean isSuperAdmin;
    private boolean isSelf;

    // Security audit log for this user
    private List<AdminDashboardView.AuditLogItemDto> auditLogs = new ArrayList<>();

    // Support tickets by this user
    private List<AdminDashboardView.TicketSummaryDto> supportTickets = new ArrayList<>();

    public AdminUserDetail() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPlatformRole() { return platformRole; }
    public void setPlatformRole(String platformRole) { this.platformRole = platformRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCreatedAtFormatted() { return createdAtFormatted; }
    public void setCreatedAtFormatted(String createdAtFormatted) { this.createdAtFormatted = createdAtFormatted; }

    public String getLastLoginFormatted() { return lastLoginFormatted; }
    public void setLastLoginFormatted(String lastLoginFormatted) { this.lastLoginFormatted = lastLoginFormatted; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public long getFamilyMemberCount() { return familyMemberCount; }
    public void setFamilyMemberCount(long familyMemberCount) { this.familyMemberCount = familyMemberCount; }

    public long getSupportTicketCount() { return supportTicketCount; }
    public void setSupportTicketCount(long supportTicketCount) { this.supportTicketCount = supportTicketCount; }

    public long getSosEventCount() { return sosEventCount; }
    public void setSosEventCount(long sosEventCount) { this.sosEventCount = sosEventCount; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public boolean isSelf() { return isSelf; }
    public void setSelf(boolean self) { isSelf = self; }

    public List<AdminDashboardView.AuditLogItemDto> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AdminDashboardView.AuditLogItemDto> auditLogs) { this.auditLogs = auditLogs; }

    public List<AdminDashboardView.TicketSummaryDto> getSupportTickets() { return supportTickets; }
    public void setSupportTickets(List<AdminDashboardView.TicketSummaryDto> supportTickets) { this.supportTickets = supportTickets; }
}
