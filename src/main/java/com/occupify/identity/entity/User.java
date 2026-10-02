package com.occupify.identity.entity;

import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("role")
    private String role;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    private boolean isNewEntity = false;

    public User() {
    }

    public User(UUID id, String email, String password, String role, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : UserRole.USER.name();
        this.status = status != null ? status : UserStatus.ACTIVE.name();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static User createNew(String email, String encodedPassword) {
        return createWithRole(email, encodedPassword, UserRole.USER.name());
    }

    public static User createInactive(String email, String encodedPassword) {
        User user = new User(
                UUID.randomUUID(),
                email,
                encodedPassword,
                UserRole.USER.name(),
                UserStatus.INACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
        user.isNewEntity = true;
        return user;
    }

    public static User createWithRole(String email, String encodedPassword, String role) {
        User user = new User(
                UUID.randomUUID(),
                email,
                encodedPassword,
                role != null ? role : UserRole.USER.name(),
                UserStatus.ACTIVE.name(),
                Instant.now(),
                Instant.now()
        );
        user.isNewEntity = true;
        return user;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntity || this.id == null;
    }

    public void markAsExisting() {
        this.isNewEntity = false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.updatedAt = Instant.now();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
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
