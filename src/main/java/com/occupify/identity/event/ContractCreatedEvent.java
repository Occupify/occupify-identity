package com.occupify.identity.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractCreatedEvent(
    UUID eventId,
    UUID contractId,
    UUID jobId,
    UUID projectId,
    UUID employerId,
    UUID employeeId,
    String freelancerEmail,
    String invitationMessage,
    String role,
    String title,
    BigDecimal agreedRate,
    String salaryType,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime createdAt)
    implements Serializable {}
