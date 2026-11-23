package Healthcare.HC.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String priority; // LOW, MEDIUM, HIGH

    @Column(nullable = false, length = 20)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SupportTicket() {
    }

    public SupportTicket(User user, String subject, String category, String priority, String status) {
        this.user = user;
        this.subject = subject;
        this.category = category;
        this.priority = priority != null ? priority : "MEDIUM";
        this.status = status != null ? status : "OPEN";
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
