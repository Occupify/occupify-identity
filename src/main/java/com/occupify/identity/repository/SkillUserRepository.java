package com.occupify.identity.repository;

import com.occupify.identity.entity.SkillUser;
import com.occupify.identity.entity.SkillUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillUserRepository extends JpaRepository<SkillUser, SkillUserId> {
    List<SkillUser> findByIdUserId(UUID userId);
    List<SkillUser> findByIdSkillId(UUID skillId);
    List<SkillUser> findByIdUserIdAndIsTopSkillTrue(UUID userId);
    Optional<SkillUser> findByIdUserIdAndIdSkillId(UUID userId, UUID skillId);
    boolean existsByIdUserIdAndIdSkillId(UUID userId, UUID skillId);
    void deleteByIdUserIdAndIdSkillId(UUID userId, UUID skillId);
}

