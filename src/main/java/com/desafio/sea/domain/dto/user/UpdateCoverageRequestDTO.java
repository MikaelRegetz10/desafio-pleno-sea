package com.desafio.sea.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record UpdateCoverageRequestDTO(
        @Schema(description = "List of state coverage codes", example = "[\"SP\", \"RJ\", \"MG\"]")
        @NotEmpty(message = "Coverage states list cannot be empty.")
        Set<@Pattern(regexp = "(?i)^[A-Z]{2}$", message = "State code must be exactly 2 letters.") String> coverageStates
) {}
