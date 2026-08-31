package com.desafio.sea.domain.dto.solicitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitationStep2DTO(
        @Schema(description = "CEP do local de atendimento", example = "70040-900")
        @NotBlank(message = "CEP is required.")
        @Pattern(regexp = "\\d{5}-\\d{3}|\\d{8}", message = "CEP must be in format 00000-000 or 8 digits.")
        String cep,

        @Schema(description = "Número do imóvel", example = "100")
        @NotBlank(message = "Number is required.")
        @Size(min = 1, max = 20, message = "Number must be between 1 and 20 characters.")
        String number,

        @Schema(description = "Complemento do endereço (opcional)", example = "Bloco A, Sala 302")
        @Size(max = 100, message = "Complement must not exceed 100 characters.")
        String complement
) {}