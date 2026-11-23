package Healthcare.HC.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicine_logs")
public class MedicineLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    public MedicineLog() {
    }

    public MedicineLog(Medicine medicine, LocalDateTime takenAt) {
        this.medicine = medicine;
        this.takenAt = takenAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.takenAt == null) {
            this.takenAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }
}
