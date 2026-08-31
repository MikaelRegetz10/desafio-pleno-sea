package com.desafio.sea.controller;

import com.desafio.sea.domain.dto.user.CreateAnalystRequestDTO;
import com.desafio.sea.domain.dto.user.UpdateCoverageRequestDTO;
import com.desafio.sea.domain.dto.user.UserResponseDTO;
import com.desafio.sea.infra.aspect.Audit;
import com.desafio.sea.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Administração", description = "Gestão de usuários, analistas e áreas de cobertura")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Audit(action = "CREATE_ANALYST")
    @Operation(summary = "Cadastrar analista", description = "Cria uma conta de usuário perfil ANALYST informando seus estados de atuação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Analista cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "E-mail já existente ou lista de UFs inválida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/analyst")
    public ResponseEntity<UserResponseDTO> createAnalyst(@Valid @RequestBody CreateAnalystRequestDTO dto) {
        UserResponseDTO response = adminService.createAnalyst(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Atualizar área de cobertura do analista", description = "Substitui a lista de UFs de atuação de um analista cadastrado.")
    @PutMapping("/{id}/coverage")
    public ResponseEntity<UserResponseDTO> updateAnalystCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoverageRequestDTO dto
    ) {
        UserResponseDTO response = adminService.updateAnalystCoverage(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todos os usuários", description = "Retorna todos os usuários cadastrados no sistema com suas respectivas coberturas.")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> listAllUsers() {
        List<UserResponseDTO> users = adminService.listAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Alternar status do usuário (Ativar/Desativar)", description = "Inverte a flag enabled do usuário informado.")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponseDTO> toggleUserStatus(@PathVariable UUID id) {
        UserResponseDTO response = adminService.toggleUserStatus(id);
        return ResponseEntity.ok(response);
    }
}