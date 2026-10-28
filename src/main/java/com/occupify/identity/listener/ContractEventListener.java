package com.occupify.identity.listener;

import com.occupify.identity.entity.Contract;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.ContractStatus;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import com.occupify.identity.event.ContractAcceptedEvent;
import com.occupify.identity.event.ContractCompensationRequestedEvent;
import com.occupify.identity.event.ContractCreatedEvent;
import com.occupify.identity.event.ContractRejectedEvent;
import com.occupify.identity.repository.ContractRepository;
import com.occupify.identity.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${app.rabbitmq.contract-queue:occupify.identity.contract.queue}")
public class ContractEventListener {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    @Transactional
    @RabbitHandler
    public void handleContractCreated(ContractCreatedEvent event) {
        if (event == null || event.contractId() == null) {
            log.warn("[Identity Saga] Received invalid ContractCreatedEvent");
            return;
        }
        log.info("[Identity Saga] Received ContractCreatedEvent for contractId: [{}]. Replica synchronization deferred until acceptance.",
                event.contractId());
        // Do not synchronize contract replica at invitation stage
    }

    @Transactional
    @RabbitHandler
    public void handleContractAccepted(ContractAcceptedEvent event) {
        log.info("[Identity Saga] Handling ContractAcceptedEvent for contractId: {}, employerId: {}, freelancerId: {}",
                event.contractId(), event.employerId(), event.freelancerId());

        Contract contract = getOrCreateContract(event);
        contract.setStatus(ContractStatus.IN_PROGRESS);
        contract.setStartedAt(Instant.now());
        contractRepository.save(contract);

        log.info("[Identity Saga] Successfully synchronized contract [{}] with status IN_PROGRESS", event.contractId());
    }

    @Transactional
    @RabbitHandler
    public void handleContractRejected(ContractRejectedEvent event) {
        log.info("[Identity Saga] Handling ContractRejectedEvent for contractId: {}, reason: {}",
                event.contractId(), event.reason());

        contractRepository.findById(event.contractId()).ifPresent(contract -> {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setFinishedAt(Instant.now());
            contractRepository.save(contract);
            log.info("[Identity Saga] Marked contract [{}] as CANCELLED", event.contractId());
        });
    }

    @Transactional
    @RabbitHandler
    public void handleContractCompensation(ContractCompensationRequestedEvent event) {
        log.warn("[Identity Saga] Handling compensation for contractId: {}, reason: {}",
                event.contractId(), event.reason());

        contractRepository.findById(event.contractId()).ifPresent(contract -> {
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setStartedAt(null);
            contractRepository.save(contract);
            log.info("[Identity Saga] Reverted contract [{}] to CANCELLED due to compensation", event.contractId());
        });
    }

    private Contract getOrCreateContract(ContractAcceptedEvent event) {
        return contractRepository.findById(event.contractId())
                .map(contract -> {
                    if (contract.getEmployee() == null && event.freelancerId() != null) {
                        userRepository.findById(event.freelancerId()).ifPresent(contract::setEmployee);
                    }
                    return contract;
                })
                .orElseGet(() -> {
            User employer = userRepository.findById(event.employerId())
                    .or(() -> userRepository.findByEmail("employer-" + event.employerId() + "@occupify.local"))
                    .orElseGet(() -> {
                        User u = new User();
                        u.setId(event.employerId());
                        u.setEmail("employer-" + event.employerId() + "@occupify.local");
                        u.setPasswordHash("N/A");
                        u.setRole(UserRole.USER.name());
                        u.setStatus(UserStatus.ACTIVE.name());
                        return userRepository.save(u);
                    });

            User employee = null;
            if (event.freelancerId() != null) {
                employee = userRepository.findById(event.freelancerId())
                        .or(() -> userRepository.findByEmail("freelancer-" + event.freelancerId() + "@occupify.local"))
                        .orElseGet(() -> {
                            User u = new User();
                            u.setId(event.freelancerId());
                            u.setEmail("freelancer-" + event.freelancerId() + "@occupify.local");
                            u.setPasswordHash("N/A");
                            u.setRole(UserRole.USER.name());
                            u.setStatus(UserStatus.ACTIVE.name());
                            return userRepository.save(u);
                        });
            }

            BigDecimal rate = event.budget() != null ? event.budget() : BigDecimal.ZERO;
            String salaryType = event.salaryType() != null ? event.salaryType() : "FIXED";
            UUID projectRoleId = event.jobId() != null ? event.jobId() : UUID.randomUUID();

            Contract newContract = new Contract(
                    event.contractId(),
                    projectRoleId,
                    employer,
                    employee,
                    "JOB_BID",
                    projectRoleId,
                    rate,
                    salaryType,
                    ContractStatus.IN_PROGRESS,
                    "PENDING",
                    null,
                    null
            );
            return contractRepository.save(newContract);
        });
    }
}
