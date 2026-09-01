package com.desafio.sea.service;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationDecisionDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationResponseDTO;
import com.desafio.sea.domain.enums.AnalysisDecision;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.repository.SolicitationRepository;
import com.desafio.sea.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalystServiceTest {

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SolicitationService solicitationService;

    @InjectMocks
    private AnalystService analystService;

    private User analyst;
    private User admin;
    private User client;
    private Solicitation mockSolicitation;

    @BeforeEach
    void setUp() {
        analyst = User.builder()
                .id(UUID.randomUUID())
                .name("Analista SP/RJ")
                .email("analista@sea.com")
                .role(Role.ANALYST)
                .coverageStates(Set.of("SP", "RJ"))
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .name("Admin")
                .email("admin@sea.com")
                .role(Role.ADMIN)
                .build();

        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente")
                .email("cliente@sea.com")
                .role(Role.CLIENT)
                .build();

        mockSolicitation = Solicitation.builder()
                .id(UUID.randomUUID())
                .client(client)
                .state("SP")
                .status(SolicitationStatus.SUBMITTED)
                .build();

        lenient().when(solicitationService.saveSolicitation(any(Solicitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnSolicitationWithinAnalystCoverage() {
        when(solicitationRepository.findById(mockSolicitation.getId())).thenReturn(Optional.of(mockSolicitation));

        SolicitationResponseDTO response = analystService.getById(mockSolicitation.getId(), analyst);

        assertNotNull(response);
        assertEquals(mockSolicitation.getId(), response.id());
    }

    @Test
    void shouldRejectSolicitationOutsideAnalystCoverage() {
        mockSolicitation.setState("MG");
        when(solicitationRepository.findById(mockSolicitation.getId())).thenReturn(Optional.of(mockSolicitation));

        assertThrows(AccessDeniedException.class, () -> analystService.getById(mockSolicitation.getId(), analyst));
    }

    @Test
    void shouldStartReviewForSubmittedSolicitation() {
        when(solicitationRepository.findById(mockSolicitation.getId())).thenReturn(Optional.of(mockSolicitation));

        SolicitationResponseDTO response = analystService.start(mockSolicitation.getId(), analyst);

        assertEquals(SolicitationStatus.IN_REVIEW, response.status());
        verify(solicitationService).saveSolicitation(mockSolicitation);
    }

    @Test
    void shouldApproveSubmittedSolicitationAndFillAuditFields() {
        when(solicitationRepository.findById(mockSolicitation.getId())).thenReturn(Optional.of(mockSolicitation));
        SolicitationDecisionDTO decisionDTO = new SolicitationDecisionDTO(AnalysisDecision.APPROVE, "Solicitação aprovada após análise.");

        SolicitationResponseDTO response = analystService.decide(mockSolicitation.getId(), analyst, decisionDTO);

        assertEquals(SolicitationStatus.APPROVED, response.status());
        assertEquals("Solicitação aprovada após análise.", response.analysisComment());
        assertNotNull(response.analyzedAt());
        verify(solicitationService).saveSolicitation(mockSolicitation);
    }

    @Test
    void shouldRejectInReviewSolicitation() {
        mockSolicitation.setStatus(SolicitationStatus.IN_REVIEW);
        when(solicitationRepository.findById(mockSolicitation.getId())).thenReturn(Optional.of(mockSolicitation));
        SolicitationDecisionDTO decisionDTO = new SolicitationDecisionDTO(AnalysisDecision.REJECT, "Reprovada após análise técnica.");

        SolicitationResponseDTO response = analystService.decide(mockSolicitation.getId(), analyst, decisionDTO);

        assertEquals(SolicitationStatus.REJECTED, response.status());
        verify(solicitationService).saveSolicitation(mockSolicitation);
    }
}