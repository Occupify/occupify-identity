package com.occupify.identity.repository;

import com.occupify.identity.entity.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, UUID> {
    List<Qualification> findByUserIdOrderByIssueDateDesc(UUID userId);
}
