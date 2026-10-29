package com.occupify.identity.entity;

import com.occupify.identity.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

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

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    // Cross-microservice reference: Không đặt FK database
    @Column(name = "project_role_id", nullable = false)
    private UUID projectRoleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private User employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private @Nullable User employee;

    @Column(name = "title", length = 255)
    private @Nullable String title;

    @Column(name = "role", length = 100)
    private @Nullable String role;

    @Column(name = "freelancer_email")
    private @Nullable String freelancerEmail;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "agreed_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal agreedRate;

    @Column(name = "salary_type", nullable = false, length = 50)
    private String salaryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContractStatus status;

    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus;

    @Column(name = "started_at")
    private @Nullable Instant startedAt;

    @Column(name = "finished_at")
    private @Nullable Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Contract(UUID id, UUID projectRoleId, User employer, @Nullable User employee, String sourceType, UUID sourceId,
                    BigDecimal agreedRate, String salaryType, ContractStatus status, String paymentStatus,
                    @Nullable Instant startedAt, @Nullable Instant finishedAt) {
        this.id = id;
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
    }

    public Contract(UUID id, UUID projectRoleId, User employer, @Nullable User employee, String sourceType, UUID sourceId,
                    BigDecimal agreedRate, String salaryType, String status, String paymentStatus,
                    @Nullable Instant startedAt, @Nullable Instant finishedAt) {
        this(id, projectRoleId, employer, employee, sourceType, sourceId, agreedRate, salaryType,
                parseStatus(status), paymentStatus, startedAt, finishedAt);
    }

    private static ContractStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return ContractStatus.IN_PROGRESS;
        }
        try {
            return ContractStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return ContractStatus.IN_PROGRESS;
        }
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
