package com.occupify.identity.repository;

import com.occupify.identity.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    Optional<Institution> findByName(String name);
    Optional<Institution> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
    List<Institution> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
    List<Institution> findTop20ByOrderByNameAsc();
}
