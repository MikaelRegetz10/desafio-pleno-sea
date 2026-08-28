package com.desafio.sea.domain.dto.user;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateCoverageRequestDTO(
        @NotEmpty(message = "Coverage states cannot be empty.")
        Set<String> coverageStates
) {
}
