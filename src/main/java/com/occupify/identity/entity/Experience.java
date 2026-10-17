package com.occupify.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "organization", nullable = false, length = 200)
    private String organization;

    @Column(name = "employment_type", length = 50)
    private @Nullable String employmentType;

    @Column(name = "location", length = 150)
    private @Nullable String location;

    @Column(name = "location_type", length = 30)
    private @Nullable String locationType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private @Nullable LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private @Nullable String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Experience(UUID id, User user, String title, String organization,
                      @Nullable String employmentType,
                      @Nullable String location,
                      @Nullable String locationType,
                      LocalDate startDate,
                      @Nullable LocalDate endDate,
                      boolean isCurrent,
                      @Nullable String description) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.organization = organization;
        this.employmentType = employmentType;
        this.location = location;
        this.locationType = locationType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Experience that = (Experience) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
