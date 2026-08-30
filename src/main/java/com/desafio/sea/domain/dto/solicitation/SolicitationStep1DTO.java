package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitationStep1DTO(
        @NotNull(message = "Service type is required.")
        ServiceType serviceType,

        @NotNull(message = "Title is required.")
        @Size(min = 3, max = 80, message = "Title length must be between 3 and 80 characters.")
        String title,

        @NotNull(message = "Description is required.")
        @Size(min = 20, max = 1000, message = "Description length must be between 20 and 1000 characters.")
        String description
) {}