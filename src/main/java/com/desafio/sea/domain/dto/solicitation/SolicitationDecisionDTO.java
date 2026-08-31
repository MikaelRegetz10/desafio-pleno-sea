package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.enums.AnalysisDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitationDecisionDTO(
        @NotNull(message = "Decision is required.")
        AnalysisDecision decision,

        @NotBlank(message = "Analysis comment is required.")
        @Size(min = 10, max = 1000, message = "Analysis comment length must be between 10 and 1000 characters.")
        String comment
) {
}
