package Healthcare.HC.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vital_readings")
public class VitalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id")
    private FamilyMember familyMember;

    @Column(nullable = false, length = 30)
    private String type; // BP, GLUCOSE, HEART_RATE, SPO2, WEIGHT

    @Column(name = "value_1", nullable = false)
    private Double value1;

    @Column(name = "value_2")
    private Double value2;

    @Column(length = 20)
    private String unit;

    @Column(length = 20)
    private String status; // NORMAL, ELEVATED, HIGH, LOW

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public VitalReading() {
    }

    public VitalReading(User user, FamilyMember familyMember, String type, Double value1, Double value2, String unit, String status, LocalDateTime recordedAt) {
        this.user = user;
        this.familyMember = familyMember;
        this.type = type;
        this.value1 = value1;
        this.value2 = value2;
        this.unit = unit;
        this.status = status;
        this.recordedAt = recordedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
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

    public FamilyMember getFamilyMember() {
        return familyMember;
    }

    public void setFamilyMember(FamilyMember familyMember) {
        this.familyMember = familyMember;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getValue1() {
        return value1;
    }

    public void setValue1(Double value1) {
        this.value1 = value1;
    }

    public Double getValue2() {
        return value2;
    }

    public void setValue2(Double value2) {
        this.value2 = value2;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
