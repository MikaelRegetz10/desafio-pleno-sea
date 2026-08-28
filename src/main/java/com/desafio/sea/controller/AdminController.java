package com.desafio.sea.controller;

import com.desafio.sea.domain.dto.user.CreateAnalystRequestDTO;
import com.desafio.sea.domain.dto.user.UpdateCoverageRequestDTO;
import com.desafio.sea.domain.dto.user.UserResponseDTO;
import com.desafio.sea.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/analyst")
    public ResponseEntity<UserResponseDTO> createAnalyst(@Valid @RequestBody CreateAnalystRequestDTO dto) {
        UserResponseDTO response = adminService.createAnalyst(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/coverage")
    public ResponseEntity<UserResponseDTO> updateAnalystCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoverageRequestDTO dto
    ) {
        UserResponseDTO response = adminService.updateAnalystCoverage(id, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> listAllUsers() {
        List<UserResponseDTO> users = adminService.listAllUsers();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponseDTO> toggleUserStatus(@PathVariable UUID id) {
        UserResponseDTO response = adminService.toggleUserStatus(id);
        return ResponseEntity.ok(response);
    }

}
