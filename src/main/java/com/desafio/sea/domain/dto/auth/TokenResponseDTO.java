package com.desafio.sea.domain.dto.auth;

import com.desafio.sea.domain.enums.Role;

import java.util.UUID;

public record TokenResponseDTO(
        String token,
        String type,
        UUID userId,
        Role role
) {
    public static TokenResponseDTO of(String token, UUID userId, Role role) {
        return new TokenResponseDTO(token, "Bearer", userId, role);
    }
}
