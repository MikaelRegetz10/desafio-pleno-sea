package com.desafio.sea.controller;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.*;
import com.desafio.sea.infra.aspect.Audit;
import com.desafio.sea.service.SolicitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/solicitations")
@Tag(name = "Solicitações (Cliente)", description = "Gerenciamento do ciclo de vida e preenchimento por etapas das solicitações")
public class SolicitationController {

    @Autowired
    private SolicitationService solicitationService;

    @Operation(summary = "Criar solicitação (Etapa 1)", description = "Inicia um rascunho de solicitação com informações básicas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Etapa 1 salva com sucesso",
                    content = @Content(schema = @Schema(implementation = SolicitationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação dos dados da etapa 1",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/step1")
    public ResponseEntity<SolicitationResponseDTO> createStep1(
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep1DTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitationService.saveStep1(null, client, dto));
    }

    @Operation(summary = "Consultar solicitação por ID", description = "Retorna os detalhes de uma solicitação pertencente ao cliente autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada",
                    content = @Content(schema = @Schema(implementation = SolicitationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitação não encontrada ou acesso negado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SolicitationResponseDTO> getById(
            @Parameter(description = "ID único da solicitação") @PathVariable UUID id,
            @AuthenticationPrincipal User client
    ) {
        return ResponseEntity.ok(solicitationService.getById(id, client));
    }

    @Operation(summary = "Atualizar Etapa 1", description = "Atualiza o título, descrição e tipo de serviço de uma solicitação em rascunho.")
    @PutMapping("/{id}/step1")
    public ResponseEntity<SolicitationResponseDTO> updateStep1(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep1DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep1(id, client, dto));
    }

    @Operation(summary = "Atualizar Etapa 2 (Endereço)", description = "Consulta o CEP via ViaCEP e vincula o endereço à solicitação.")
    @PutMapping("/{id}/step2")
    public ResponseEntity<SolicitationResponseDTO> updateStep2(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep2DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep2(id, client, dto));
    }

    @Operation(summary = "Atualizar Etapa 3 (Detalhes Finais)", description = "Define a prioridade, data desejada, valor estimado e aceite dos termos.")
    @PutMapping("/{id}/step3")
    public ResponseEntity<SolicitationResponseDTO> updateStep3(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client,
            @Valid @RequestBody SolicitationStep3DTO dto
    ) {
        return ResponseEntity.ok(solicitationService.saveStep3(id, client, dto));
    }

    @Audit(action = "SUBMIT_SOLICITATION")
    @Operation(summary = "Submeter solicitação", description = "Valida todas as etapas concluídas e altera o status para SUBMITTED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação submetida com sucesso",
                    content = @Content(schema = @Schema(implementation = SolicitationResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Regra de negócio violada ou etapas incompletas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/submit")
    public ResponseEntity<SolicitationResponseDTO> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal User client
    ) {
        return ResponseEntity.ok(solicitationService.submit(id, client));
    }
}