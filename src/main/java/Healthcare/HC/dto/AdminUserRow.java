package Healthcare.HC.dto;

/**
 * DTO for Admin Users table rows.
 * Account data only. Zero health data, zero password hashes.
 */
public class AdminUserRow {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String platformRole;
    private String status;
    private String createdAtFormatted;
    private String lastLoginFormatted;
    private String initials;
    private boolean isSuperAdmin;
    private boolean isSelf;

    public AdminUserRow() {
    }

    public AdminUserRow(Long id, String fullName, String email, String role, String platformRole,
                        String status, String createdAtFormatted, String lastLoginFormatted,
                        String initials, boolean isSuperAdmin, boolean isSelf) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.platformRole = platformRole;
        this.status = status;
        this.createdAtFormatted = createdAtFormatted;
        this.lastLoginFormatted = lastLoginFormatted;
        this.initials = initials;
        this.isSuperAdmin = isSuperAdmin;
        this.isSelf = isSelf;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPlatformRole() { return platformRole; }
    public void setPlatformRole(String platformRole) { this.platformRole = platformRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAtFormatted() { return createdAtFormatted; }
    public void setCreatedAtFormatted(String createdAtFormatted) { this.createdAtFormatted = createdAtFormatted; }

    public String getLastLoginFormatted() { return lastLoginFormatted; }
    public void setLastLoginFormatted(String lastLoginFormatted) { this.lastLoginFormatted = lastLoginFormatted; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public boolean isSelf() { return isSelf; }
    public void setSelf(boolean self) { isSelf = self; }
}
