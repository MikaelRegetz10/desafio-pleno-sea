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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analyst/solicitations")
@Tag(name = "Solicitações (Analista)", description = "Fila de trabalho, busca avançada via Elasticsearch e análise de solicitações")
public class AnalystController {

    @Autowired
    private AnalystService analystService;

    @Autowired
    private SolicitationSearchService solicitationSearchService;

    @Operation(summary = "Buscar solicitações (Elasticsearch)", description = "Pesquisa paginada e filtrada restrita aos estados de cobertura do analista.")
    @GetMapping("/search")
    public ResponseEntity<PageResponseDTO<SolicitationDocument>> search(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Termo de busca textual em título ou descrição") @RequestParam(required = false) String q,
            @Parameter(description = "Filtro por lista de status") @RequestParam(required = false) List<SolicitationStatus> status,
            @Parameter(description = "Filtro por tipo de serviço") @RequestParam(required = false) ServiceType serviceType,
            @Parameter(description = "Filtro por prioridade") @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filtro por UF") @RequestParam(required = false) String state,
            @Parameter(description = "Data inicial (submissão)") @RequestParam(required = false) Instant dateFrom,
            @Parameter(description = "Data final (submissão)") @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt desc") String sort
    ) {
        PageResponseDTO<SolicitationDocument> response = solicitationSearchService.search(
                user, q, status, serviceType, priority, state, dateFrom, dateTo, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar detalhes da solicitação", description = "Retorna os dados completos de uma solicitação dentro da área de cobertura do analista.")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitationResponseDTO> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst
    ) {
        SolicitationResponseDTO response = analystService.getById(id, analyst);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Iniciar análise", description = "Altera o status da solicitação de SUBMITTED para IN_REVIEW.")
    @PostMapping("/{id}/start")
    public ResponseEntity<SolicitationResponseDTO> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst
    ) {
        return ResponseEntity.ok(analystService.start(id, analyst));
    }

    @Audit(action = "DECIDE_SOLICITATION")
    @Operation(summary = "Decidir solicitação (Aprovar/Rejeitar)", description = "Registra parecer do analista, definindo o status para APPROVED ou REJECTED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decisão gravada com sucesso",
                    content = @Content(schema = @Schema(implementation = SolicitationResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Solicitação fora da cobertura do analista",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/decide")
    public ResponseEntity<SolicitationResponseDTO> decide(
            @PathVariable UUID id,
            @AuthenticationPrincipal User analyst,
            @Valid @RequestBody SolicitationDecisionDTO dto
    ) {
        return ResponseEntity.ok(analystService.decide(id, analyst, dto));
    }
}