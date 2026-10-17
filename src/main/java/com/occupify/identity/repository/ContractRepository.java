package com.occupify.identity.repository;

import com.occupify.identity.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findByEmployerId(UUID employerId);
    List<Contract> findByEmployeeId(UUID employeeId);
    List<Contract> findByProjectRoleId(UUID projectRoleId);
    List<Contract> findByStatus(String status);
}
