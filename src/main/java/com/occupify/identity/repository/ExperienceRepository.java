package com.occupify.identity.repository;

import com.occupify.identity.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, UUID> {
    List<Experience> findByUserIdOrderByStartDateDesc(UUID userId);
    Optional<Experience> findByIdAndUserId(UUID id, UUID userId);
}

