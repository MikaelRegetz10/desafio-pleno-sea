package com.desafio.sea.domain.dto.solicitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitationStep2DTO(
        @NotBlank(message = "CEP is required.")
        @Pattern(regexp = "\\d{5}-\\d{3}|\\d{8}", message = "CEP must be in format 00000-000 or 8 digits.")
        String cep,

        @NotBlank(message = "Number is required.")
        @Size(min = 1, max = 20, message = "Number must be between 1 and 20 characters.")
        String number,

        @Size(max = 100, message = "Complement must not exceed 100 characters.")
        String complement
) {}