package com.desafio.sea.controller;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.PageResponseDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationDecisionDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationResponseDTO;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.infra.aspect.Audit;
import com.desafio.sea.infra.elasticsearch.SolicitationDocument;
import com.desafio.sea.service.AnalystService;
import com.desafio.sea.service.SolicitationSearchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analyst/solicitations")
public class AnalystController {

    @Autowired
    private AnalystService analystService;

    @Autowired
    private SolicitationSearchService solicitationSearchService;

    @GetMapping("/search")
    public ResponseEntity<PageResponseDTO<SolicitationDocument>> search(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<SolicitationStatus> status,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt desc") String sort
    ) {
        PageResponseDTO<SolicitationDocument> response = solicitationSearchService.search(
                user, q, status, serviceType, priority, state, dateFrom, dateTo, page, size, sort
        );
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

    @Audit(action = "DECIDE_SOLICITATION")
    @PostMapping("/{id}/decide")
    public ResponseEntity<SolicitationResponseDTO> decide(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst,
            @Valid @RequestBody SolicitationDecisionDTO dto
    ) {
        return ResponseEntity.ok(analystService.decide(id, analyst, dto));
    }
}
