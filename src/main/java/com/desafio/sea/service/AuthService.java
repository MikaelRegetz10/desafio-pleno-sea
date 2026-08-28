package com.desafio.sea.service;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.auth.LoginRequestDTO;
import com.desafio.sea.domain.dto.auth.TokenResponseDTO;
import com.desafio.sea.domain.dto.user.UserRequestDTO;
import com.desafio.sea.infra.security.TokenService;
import com.desafio.sea.repository.UserRepository;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public TokenResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas."));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Usuário desativado.");
        }

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }

        String token= tokenService.generateToken(user);
        return TokenResponseDTO.of(token, user.getId(), user.getRole());
    }

}
