package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitationStep1DTO(
        @Schema(description = "Tipo do serviço solicitado", example = "INSTALLATION")
        @NotNull(message = "Service type is required.")
        ServiceType serviceType,

        @Schema(description = "Título resumido da solicitação", example = "Instalação de Painel Solar")
        @NotNull(message = "Title is required.")
        @Size(min = 3, max = 80, message = "Title length must be between 3 and 80 characters.")
        String title,

        @Schema(description = "Descrição detalhada da demanda", example = "Instalação completa de módulos fotovoltaicos na cobertura do edifício.")
        @NotNull(message = "Description is required.")
        @Size(min = 20, max = 1000, message = "Description length must be between 20 and 1000 characters.")
        String description
) {}