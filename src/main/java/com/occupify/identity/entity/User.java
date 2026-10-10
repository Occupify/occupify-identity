package com.occupify.identity.entity;

import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "headline", length = 255)
    private String headline;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public User(UUID id, String email, String passwordHash, String role, String status, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.EMPLOYEE.name();
        this.status = status != null ? status : UserStatus.ACTIVE.name();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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
        if (role == null) {
            role = UserRole.EMPLOYEE.name();
        }
        if (status == null) {
            status = UserStatus.ACTIVE.name();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Backward-compatibility helpers for existing auth service code
    public String getPassword() {
        return this.passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
        this.updatedAt = Instant.now();
    }

    public static User createNew(String email, String encodedPassword) {
        return createWithRole(email, encodedPassword, UserRole.EMPLOYEE.name());
    }

    public static User createInactive(String email, String encodedPassword) {
        return new User(
                UUID.randomUUID(),
                email,
                encodedPassword,
                UserRole.EMPLOYEE.name(),
                UserStatus.INACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
    }

    public static User createWithRole(String email, String encodedPassword, String role) {
        return new User(
                UUID.randomUUID(),
                email,
                encodedPassword,
                role != null ? role : UserRole.EMPLOYEE.name(),
                UserStatus.ACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
