package com.desafio.sea.service;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.user.CreateAnalystRequestDTO;
import com.desafio.sea.domain.dto.user.UpdateCoverageRequestDTO;
import com.desafio.sea.domain.dto.user.UserResponseDTO;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO createAnalyst(CreateAnalystRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email().toLowerCase().trim())) {
            throw new IllegalArgumentException("Email is already registered in the system.");
        }

        Set<String> normalizedStates = dto.coverageStates().stream()
                .map(String::toUpperCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        User analyst = User.builder()
                .name(dto.name().trim())
                .email(dto.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .role(Role.ANALYST)
                .coverageStates(normalizedStates)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(analyst);
        return UserResponseDTO.fromEntity(savedUser);
    }

    @Transactional
    public UserResponseDTO updateAnalystCoverage(UUID analystId, UpdateCoverageRequestDTO dto) {
        User analyst = userRepository.findById(analystId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + analystId));

        if (analyst.getRole() != Role.ANALYST) {
            throw new IllegalArgumentException("Coverage states can only be updated for ANALYST users.");
        }

        Set<String> normalizedStates = dto.coverageStates().stream()
                .map(String::toUpperCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        analyst.setCoverageStates(normalizedStates);
        return UserResponseDTO.fromEntity(userRepository.save(analyst));
    }

    public List<UserResponseDTO> listAllUsers() {
        return userRepository.findAllWithCoverageStates().stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
    }

    public UserResponseDTO toggleUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setEnabled(!user.isEnabled());
        return UserResponseDTO.fromEntity(userRepository.save(user));
    }

}
