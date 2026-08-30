package com.desafio.sea.controller;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.*;
import com.desafio.sea.service.SolicitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/solicitations")
public class SolicitationController {

    @Autowired
    private SolicitationService solicitationService;

    @PostMapping("/step1")
    public ResponseEntity<SolicitationResponseDTO> createStep1(
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep1DTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitationService.saveStep1(null, client, dto));
    }

    @PutMapping("/{id}/step1")
    public ResponseEntity<SolicitationResponseDTO> updateStep1(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep1DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep1(id, client, dto));
    }

    @PutMapping("/{id}/step2")
    public ResponseEntity<SolicitationResponseDTO> updateStep2(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep2DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep2(id, client, dto));
    }

    @PutMapping("/{id}/step3")
    public ResponseEntity<SolicitationResponseDTO> updateStep3(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep3DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep3(id, client, dto));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SolicitationResponseDTO> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client
    ) {
        return ResponseEntity.ok(solicitationService.submit(id, client));
    }
}