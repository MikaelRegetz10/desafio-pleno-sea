package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SolicitationResponseDTO(
        UUID id,
        UUID clientId,
        SolicitationStatus status,
        Integer currentStep,
        ServiceType serviceType,
        String title,
        String description,
        String cep,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        Boolean termsAccepted,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant analyzedAt,
        UUID analyzedBy,
        String analysisComment
) {
    public static SolicitationResponseDTO fromEntity(Solicitation entity) {
        return new SolicitationResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getStatus(),
                entity.getCurrentStep(),
                entity.getServiceType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCep(),
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState(),
                entity.getPriority(),
                entity.getPreferredDate(),
                entity.getEstimatedValue(),
                entity.getTermsAccepted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSubmittedAt(),
                entity.getAnalyzedAt(),
                entity.getAnalyzedBy() != null ? entity.getAnalyzedBy().getId() : null,
                entity.getAnalysisComment()
        );
    }
}