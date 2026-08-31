package com.desafio.sea.controller;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationDecisionDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationResponseDTO;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.service.AnalystService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analyst/solicitations")
public class AnalystController {

    @Autowired
    private AnalystService analystService;

    @GetMapping
    public ResponseEntity<List<SolicitationResponseDTO>> listAll(
            @AuthenticationPrincipal User analyst,
            @RequestParam(required = false) SolicitationStatus status
    ) {
        List<SolicitationResponseDTO> response = analystService.listAvailableForAnalyst(analyst, status);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<SolicitationResponseDTO> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst
    ) {
        SolicitationResponseDTO response = analystService.getById(id, analyst);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SolicitationResponseDTO> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst
    ) {
        return ResponseEntity.ok(analystService.start(id, analyst));
    }

    @PostMapping("/{id}/decide")
    public ResponseEntity<SolicitationResponseDTO> decide(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst,
            @Valid @RequestBody SolicitationDecisionDTO dto
    ) {
        return ResponseEntity.ok(analystService.decide(id, analyst, dto));
    }
}
