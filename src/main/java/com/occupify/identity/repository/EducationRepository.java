package com.occupify.identity.repository;

import com.occupify.identity.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EducationRepository extends JpaRepository<Education, UUID> {
    List<Education> findByUserIdOrderByStartDateDesc(UUID userId);
}
