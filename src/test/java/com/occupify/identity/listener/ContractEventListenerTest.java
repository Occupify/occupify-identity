package com.occupify.identity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractEventListenerTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContractEventListener contractEventListener;

    private final UUID employerId = UUID.randomUUID();
    private final UUID freelancerId = UUID.randomUUID();
    private final UUID contractId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @Test
    @DisplayName("Should not synchronize contract on ContractCreatedEvent (deferred until acceptance)")
    void handleContractCreated_DefersSynchronization() {
        ContractCreatedEvent event = new ContractCreatedEvent(
                UUID.randomUUID(), contractId, jobId, UUID.randomUUID(),
                employerId, freelancerId, "free@occupify.local", "Welcome",
                "Frontend Lead", "React Dev", new BigDecimal("500.00"),
                "FIXED", null, null, LocalDateTime.now());

        contractEventListener.handleContractCreated(event);

        verify(contractRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should synchronize contract with status IN_PROGRESS on ContractAcceptedEvent")
    void handleContractAccepted_SetsStatusInProgress() {
        User employer = new User(employerId, "emp@occupify.local", "hash", UserRole.USER.name(), UserStatus.ACTIVE.name(), null, null);
        User employee = new User(freelancerId, "free@occupify.local", "hash", UserRole.USER.name(), UserStatus.ACTIVE.name(), null, null);
        Contract contract = new Contract(contractId, jobId, employer, employee, "JOB_BID", jobId, new BigDecimal("450.00"), "FIXED", ContractStatus.IN_PROGRESS, "PENDING", null, null);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        ContractAcceptedEvent event = new ContractAcceptedEvent(
                UUID.randomUUID(), contractId, jobId, UUID.randomUUID(),
                employerId, freelancerId, "Web Dev Contract", new BigDecimal("450.00"),
                "FIXED", LocalDate.now(), LocalDate.now().plusMonths(2), LocalDateTime.now());

        contractEventListener.handleContractAccepted(event);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
        assertThat(contract.getStartedAt()).isNotNull();
        verify(contractRepository).save(contract);
    }

    @Test
    @DisplayName("Should mark contract as CANCELLED on ContractRejectedEvent")
    void handleContractRejected_SetsStatusCancelled() {
        Contract contract = new Contract(contractId, jobId, null, null, "JOB_BID", jobId, BigDecimal.ZERO, "FIXED", ContractStatus.IN_PROGRESS, "PENDING", null, null);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        ContractRejectedEvent event = new ContractRejectedEvent(
                UUID.randomUUID(), contractId, jobId, employerId, freelancerId, "Budget too low", LocalDateTime.now());

        contractEventListener.handleContractRejected(event);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
        assertThat(contract.getFinishedAt()).isNotNull();
        verify(contractRepository).save(contract);
    }

    @Test
    @DisplayName("Should revert contract status to CANCELLED on ContractCompensationRequestedEvent")
    void handleContractCompensation_RevertsStatusCancelled() {
        Contract contract = new Contract(contractId, jobId, null, null, "JOB_BID", jobId, BigDecimal.ZERO, "FIXED", ContractStatus.IN_PROGRESS, "PENDING", null, null);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        ContractCompensationRequestedEvent event = new ContractCompensationRequestedEvent(
                UUID.randomUUID(), contractId, "occupify-payment", "INSUFFICIENT_FUNDS", "Payment failed", LocalDateTime.now());

        contractEventListener.handleContractCompensation(event);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
        assertThat(contract.getStartedAt()).isNull();
        verify(contractRepository).save(contract);
    }
}
