package com.occupify.identity.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "skills_users")
@Getter
@Setter
@NoArgsConstructor
public class SkillUser {

    @EmbeddedId
    private SkillUserId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    public SkillUser(User user, Skill skill) {
        this.user = user;
        this.skill = skill;
        this.id = new SkillUserId(user.getId(), skill.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillUser skillUser = (SkillUser) o;
        return Objects.equals(id, skillUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
