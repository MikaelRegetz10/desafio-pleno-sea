package com.desafio.sea.domain.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequestDTO(
        @NotBlank(message = "Name is required.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a well-formed email address.")
        String email,

        @NotBlank(message = "Password is required.")
        String password
) {
}
