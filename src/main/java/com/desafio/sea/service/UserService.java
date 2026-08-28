package com.desafio.sea.service;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.user.UserRequestDTO;
import com.desafio.sea.domain.dto.user.UserResponseDTO;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserResponseDTO createClient(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())){
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(dto.name().trim())
                .email(dto.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .role(Role.CLIENT)
                .build();

        User userSave = userRepository.save(user);

        return UserResponseDTO.fromEntity(userSave);

    }
}
