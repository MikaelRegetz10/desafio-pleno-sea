package com.desafio.sea.domain.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateAnalystRequestDTO(
        @NotBlank(message = "Name is required.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a well-formed email address.")
        String email,

        @NotBlank(message = "Password is required.")
        String password,

        @NotEmpty(message = "At least one coverage state is required.")
        Set<String> coverageStates
) {
}
