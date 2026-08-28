package com.desafio.sea.controller;

import com.desafio.sea.domain.dto.auth.LoginRequestDTO;
import com.desafio.sea.domain.dto.auth.TokenResponseDTO;
import com.desafio.sea.domain.dto.user.UserRequestDTO;
import com.desafio.sea.domain.dto.user.UserResponseDTO;
import com.desafio.sea.service.AuthService;
import com.desafio.sea.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.createClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto) {
        TokenResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }
}
