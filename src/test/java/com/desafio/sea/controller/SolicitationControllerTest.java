package com.desafio.sea.controller;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationResponseDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep1DTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep2DTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep3DTO;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.service.SolicitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitationControllerTest {

    @Mock
    private SolicitationService solicitationService;

    @InjectMocks
    private SolicitationController solicitationController;

    private User client;
    private UUID solicitationId;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente Teste")
                .email("cliente@sea.com")
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        solicitationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve criar o step 1 e retornar HTTP 201")
    void shouldCreateStep1() {
        SolicitationStep1DTO dto = validStep1();
        SolicitationResponseDTO serviceResponse = response(1, SolicitationStatus.DRAFT);
        when(solicitationService.saveStep1(null, client, dto)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response = solicitationController.createStep1(client, dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).saveStep1(null, client, dto);
        verifyNoMoreInteractions(solicitationService);
    }

    @Test
    @DisplayName("Deve buscar uma solicitação e retornar HTTP 200")
    void shouldGetSolicitation() {
        SolicitationResponseDTO serviceResponse = response(2, SolicitationStatus.DRAFT);
        when(solicitationService.getById(solicitationId, client)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response =
                solicitationController.getById(solicitationId, client);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).getById(solicitationId, client);
        verifyNoMoreInteractions(solicitationService);
    }

    @Test
    @DisplayName("Deve atualizar o step 1 e retornar HTTP 200")
    void shouldUpdateStep1() {
        SolicitationStep1DTO dto = validStep1();
        SolicitationResponseDTO serviceResponse = response(1, SolicitationStatus.DRAFT);
        when(solicitationService.saveStep1(solicitationId, client, dto)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response =
                solicitationController.updateStep1(solicitationId, client, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).saveStep1(solicitationId, client, dto);
        verifyNoMoreInteractions(solicitationService);
    }

    @Test
    @DisplayName("Deve atualizar o step 2 e retornar HTTP 200")
    void shouldUpdateStep2() {
        SolicitationStep2DTO dto = new SolicitationStep2DTO("70040-900", "100", "Bloco A");
        SolicitationResponseDTO serviceResponse = response(2, SolicitationStatus.DRAFT);
        when(solicitationService.saveStep2(solicitationId, client, dto)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response =
                solicitationController.updateStep2(solicitationId, client, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).saveStep2(solicitationId, client, dto);
        verifyNoMoreInteractions(solicitationService);
    }

    @Test
    @DisplayName("Deve atualizar o step 3 e retornar HTTP 200")
    void shouldUpdateStep3() {
        SolicitationStep3DTO dto = validStep3();
        SolicitationResponseDTO serviceResponse = response(3, SolicitationStatus.DRAFT);
        when(solicitationService.saveStep3(solicitationId, client, dto)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response =
                solicitationController.updateStep3(solicitationId, client, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).saveStep3(solicitationId, client, dto);
        verifyNoMoreInteractions(solicitationService);
    }

    @Test
    @DisplayName("Deve submeter a solicitação e retornar HTTP 200")
    void shouldSubmitSolicitation() {
        SolicitationResponseDTO serviceResponse = response(3, SolicitationStatus.SUBMITTED);
        when(solicitationService.submit(solicitationId, client)).thenReturn(serviceResponse);

        ResponseEntity<SolicitationResponseDTO> response =
                solicitationController.submit(solicitationId, client);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(solicitationService).submit(solicitationId, client);
        verifyNoMoreInteractions(solicitationService);
    }

    private SolicitationStep1DTO validStep1() {
        return new SolicitationStep1DTO(
                ServiceType.INSTALLATION,
                "Instalação Solar",
                "Descrição válida do projeto de instalação elétrica."
        );
    }

    private SolicitationStep3DTO validStep3() {
        return new SolicitationStep3DTO(
                Priority.MEDIUM,
                LocalDate.now().plusDays(2),
                new BigDecimal("250.00"),
                true
        );
    }

    private SolicitationResponseDTO response(int currentStep, SolicitationStatus status) {
        return new SolicitationResponseDTO(
                solicitationId,
                client.getId(),
                status,
                currentStep,
                ServiceType.INSTALLATION,
                "Instalação Solar",
                "Descrição válida do projeto de instalação elétrica.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
