package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.enums.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitationStep3DTO(
        @NotNull(message = "Priority is required.")
        Priority priority,

        @NotNull(message = "Preferred date is required.")
        @FutureOrPresent(message = "Preferred date must not be in the past.")
        LocalDate preferredDate,

        @NotNull(message = "Estimated value is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Estimated value must be greater than or equal to 0.")
        BigDecimal estimatedValue,

        @NotNull(message = "Terms acceptance is required.")
        @AssertTrue(message = "Terms must be accepted to proceed.")
        Boolean termsAccepted
) {}