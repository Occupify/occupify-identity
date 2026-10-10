package com.occupify.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class Contract {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Cross-microservice reference: Không đặt FK database
    @Column(name = "project_role_id", nullable = false)
    private UUID projectRoleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private User employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "agreed_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal agreedRate;

    @Column(name = "salary_type", nullable = false, length = 50)
    private String salaryType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Contract(UUID id, UUID projectRoleId, User employer, User employee, String sourceType, UUID sourceId,
                    BigDecimal agreedRate, String salaryType, String status, String paymentStatus,
                    Instant startedAt, Instant finishedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.projectRoleId = projectRoleId;
        this.employer = employer;
        this.employee = employee;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.agreedRate = agreedRate;
        this.salaryType = salaryType;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return Objects.equals(id, contract.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
