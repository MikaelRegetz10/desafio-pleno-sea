package com.desafio.sea.domain.dto.user;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.enums.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        Role role,
        boolean enabled,
        Set<String> coverageStates,
        Instant createdAt
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCoverageStates(),
                user.getCreatedAt()
        );
    }
}
