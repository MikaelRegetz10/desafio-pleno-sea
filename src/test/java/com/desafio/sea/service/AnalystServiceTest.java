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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalystServiceTest {

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalystService analystService;

    private Solicitation solicitation;
    private User analyst;
    private User admin;

    @BeforeEach
    void setUp() {
        User client = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        solicitation = Solicitation.builder()
                .id(UUID.randomUUID())
                .client(client)
                .state("SP")
                .status(SolicitationStatus.SUBMITTED)
                .build();
        analyst = User.builder()
                .id(UUID.randomUUID())
                .role(Role.ANALYST)
                .coverageStates(Set.of("SP", "RJ"))
                .build();
        admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
    }

    @Test
    void shouldReturnSolicitationWithinAnalystCoverage() {
        mockSolicitation();

        SolicitationResponseDTO response = analystService.getById(solicitation.getId(), analyst);

        assertEquals(solicitation.getId(), response.id());
        assertEquals("SP", response.state());
    }

    @Test
    void shouldRejectSolicitationOutsideAnalystCoverage() {
        solicitation.setState("MG");
        mockSolicitation();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> analystService.getById(solicitation.getId(), analyst)
        );

        assertEquals("Access denied. Solicitation state is outside your coverage.", exception.getMessage());
    }

    @Test
    void shouldAllowAdminToAccessAnyState() {
        solicitation.setState("MG");
        mockSolicitation();

        SolicitationResponseDTO response = analystService.getById(solicitation.getId(), admin);

        assertEquals(solicitation.getId(), response.id());
    }

    @Test
    void shouldStartReviewForSubmittedSolicitation() {
        mockSolicitation();
        when(solicitationRepository.save(solicitation)).thenReturn(solicitation);

        SolicitationResponseDTO response = analystService.start(solicitation.getId(), analyst);

        assertEquals(SolicitationStatus.IN_REVIEW, solicitation.getStatus());
        assertEquals(SolicitationStatus.IN_REVIEW, response.status());
        verify(solicitationRepository).save(solicitation);
    }

    @Test
    void shouldNotStartReviewForSolicitationOutsideSubmittedStatus() {
        solicitation.setStatus(SolicitationStatus.DRAFT);
        mockSolicitation();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> analystService.start(solicitation.getId(), analyst)
        );

        assertEquals("Only SUBMITTED solicitations can be started for review.", exception.getMessage());
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    void shouldApproveSubmittedSolicitationAndFillAuditFields() {
        mockSolicitation();
        when(solicitationRepository.save(solicitation)).thenReturn(solicitation);
        SolicitationDecisionDTO dto = new SolicitationDecisionDTO(
                AnalysisDecision.APPROVE, "  Solicitação aprovada após análise.  "
        );

        SolicitationResponseDTO response = analystService.decide(solicitation.getId(), analyst, dto);

        assertEquals(SolicitationStatus.APPROVED, solicitation.getStatus());
        assertEquals("Solicitação aprovada após análise.", solicitation.getAnalysisComment());
        assertEquals(analyst, solicitation.getAnalyzedBy());
        assertNotNull(solicitation.getAnalyzedAt());
        assertEquals(SolicitationStatus.APPROVED, response.status());
        verify(solicitationRepository).save(solicitation);
    }

    @Test
    void shouldRejectInReviewSolicitation() {
        solicitation.setStatus(SolicitationStatus.IN_REVIEW);
        mockSolicitation();
        when(solicitationRepository.save(solicitation)).thenReturn(solicitation);

        analystService.decide(solicitation.getId(), analyst,
                new SolicitationDecisionDTO(AnalysisDecision.REJECT, "Reprovada após análise técnica."));

        assertEquals(SolicitationStatus.REJECTED, solicitation.getStatus());
    }

    @Test
    void shouldNotDecideDraftSolicitation() {
        solicitation.setStatus(SolicitationStatus.DRAFT);
        mockSolicitation();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> analystService.decide(solicitation.getId(), analyst,
                        new SolicitationDecisionDTO(AnalysisDecision.APPROVE, "Aprovação não permitida agora."))
        );

        assertEquals("Only SUBMITTED or IN_REVIEW solicitations can be decided.", exception.getMessage());
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSolicitationNotFound() {
        UUID notFoundId = UUID.randomUUID();
        when(solicitationRepository.findById(notFoundId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analystService.getById(notFoundId, analyst)
        );

        assertEquals("Solicitation not found with id: " + notFoundId, exception.getMessage());
    }

    @Test
    void shouldRejectAccessForClientUser() {
        User clientUser = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        mockSolicitation();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> analystService.getById(solicitation.getId(), clientUser)
        );

        assertEquals("Only ANALYST or ADMIN users can access solicitations for analysis.", exception.getMessage());
    }

    @Test
    void shouldRejectAnalystWhenSolicitationStateIsNull() {
        solicitation.setState(null);
        mockSolicitation();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> analystService.getById(solicitation.getId(), analyst)
        );

        assertEquals("Access denied. Solicitation state is outside your coverage.", exception.getMessage());
    }

    @Test
    void shouldListSolicitationsWithinAnalystCoverage() {
        mockUserRepositoryForAnalyst();
        when(solicitationRepository.findByStateInAndOptionalStatus(analyst.getCoverageStates(), null))
                .thenReturn(List.of(solicitation));

        List<SolicitationResponseDTO> response = analystService.listAvailableForAnalyst(analyst, null);

        assertEquals(1, response.size());
        assertEquals(solicitation.getId(), response.get(0).id());
    }

    @Test
    void shouldListAllSolicitationsForAdmin() {
        mockUserRepositoryForAdmin();
        when(solicitationRepository.findByOptionalStatus(null))
                .thenReturn(List.of(solicitation));

        List<SolicitationResponseDTO> response = analystService.listAvailableForAnalyst(admin, null);

        assertEquals(1, response.size());
    }

    @Test
    void shouldReturnEmptyListWhenAnalystHasNoCoverageStates() {
        User emptyCoverageAnalyst = User.builder()
                .id(UUID.randomUUID())
                .role(Role.ANALYST)
                .coverageStates(Set.of())
                .build();

        when(userRepository.findById(emptyCoverageAnalyst.getId())).thenReturn(Optional.of(emptyCoverageAnalyst));

        List<SolicitationResponseDTO> response = analystService.listAvailableForAnalyst(emptyCoverageAnalyst, null);

        assertTrue(response.isEmpty());
        verify(solicitationRepository, never()).findByStateInAndOptionalStatus(any(), any());
    }

    @Test
    void shouldDenyListingForClientRole() {
        User clientUser = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));

        assertThrows(
                AccessDeniedException.class,
                () -> analystService.listAvailableForAnalyst(clientUser, null)
        );
    }

    private void mockUserRepositoryForAnalyst() {
        when(userRepository.findById(analyst.getId())).thenReturn(Optional.of(analyst));
    }

    private void mockUserRepositoryForAdmin() {
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    }

    private void mockSolicitation() {
        when(solicitationRepository.findById(solicitation.getId())).thenReturn(Optional.of(solicitation));
    }
}
