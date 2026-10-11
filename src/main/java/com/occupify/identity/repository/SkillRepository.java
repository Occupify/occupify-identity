package com.occupify.identity.repository;

import com.occupify.identity.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByName(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
    List<Skill> findByNameContainingIgnoreCase(String keyword);
}

