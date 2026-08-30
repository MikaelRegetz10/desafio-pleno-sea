package com.desafio.sea.service;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationResponseDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep1DTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep2DTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationStep3DTO;
import com.desafio.sea.domain.dto.solicitation.ViaCepResponseDTO;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.infra.client.ViaCepService;
import com.desafio.sea.repository.SolicitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    private static final String VALID_DESCRIPTION =
            "Descrição detalhada da solicitação com mais de vinte caracteres.";

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private ViaCepService viaCepService;

    @InjectMocks
    private SolicitationService solicitationService;

    private User client;
    private User anotherClient;
    private Solicitation draftSolicitation;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente Teste")
                .email("cliente@sea.com")
                .enabled(true)
                .build();
        anotherClient = User.builder()
                .id(UUID.randomUUID())
                .name("Outro Cliente")
                .email("outro@sea.com")
                .enabled(true)
                .build();
        draftSolicitation = Solicitation.builder()
                .id(UUID.randomUUID())
                .client(client)
                .status(SolicitationStatus.DRAFT)
                .currentStep(1)
                .serviceType(ServiceType.INSTALLATION)
                .title("Instalação Solar")
                .description(VALID_DESCRIPTION)
                .build();
    }

    @Test
    @DisplayName("Deve criar uma solicitação no step 1")
    void shouldCreateSolicitationAtStep1() {
        SolicitationStep1DTO dto = new SolicitationStep1DTO(
                ServiceType.MAINTENANCE,
                "  Manutenção Elétrica  ",
                "  Descrição válida da manutenção elétrica solicitada.  "
        );
        mockRepositorySave();

        SolicitationResponseDTO response = solicitationService.saveStep1(null, client, dto);

        ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
        verify(solicitationRepository).save(captor.capture());
        Solicitation saved = captor.getValue();
        assertSame(client, saved.getClient());
        assertEquals(SolicitationStatus.DRAFT, saved.getStatus());
        assertEquals(1, saved.getCurrentStep());
        assertEquals(ServiceType.MAINTENANCE, saved.getServiceType());
        assertEquals("Manutenção Elétrica", saved.getTitle());
        assertEquals("Descrição válida da manutenção elétrica solicitada.", saved.getDescription());
        assertEquals(saved.getId(), response.id());
        assertEquals(client.getId(), response.clientId());
        verify(solicitationRepository, never()).findById(any());
        verifyNoInteractions(viaCepService);
    }

    @Test
    @DisplayName("Deve atualizar o step 1 sem regredir a etapa atual")
    void shouldUpdateStep1WithoutRegressingCurrentStep() {
        draftSolicitation.setCurrentStep(3);
        SolicitationStep1DTO dto = new SolicitationStep1DTO(
                ServiceType.INSPECTION,
                "  Inspeção Técnica  ",
                "  Descrição suficientemente longa para uma inspeção técnica.  "
        );
        mockExistingDraft();
        mockRepositorySave();

        SolicitationResponseDTO response =
                solicitationService.saveStep1(draftSolicitation.getId(), client, dto);

        assertEquals(3, draftSolicitation.getCurrentStep());
        assertEquals(ServiceType.INSPECTION, draftSolicitation.getServiceType());
        assertEquals("Inspeção Técnica", draftSolicitation.getTitle());
        assertEquals("Descrição suficientemente longa para uma inspeção técnica.",
                draftSolicitation.getDescription());
        assertEquals(3, response.currentStep());
        verify(solicitationRepository).save(draftSolicitation);
    }

    @Test
    @DisplayName("Deve consultar o ViaCEP, preencher o endereço e avançar para o step 2")
    void shouldSaveStep2WithAddressReturnedByViaCep() {
        SolicitationStep2DTO dto = new SolicitationStep2DTO(
                "70040-900",
                "  100  ",
                "  Bloco A  "
        );
        ViaCepResponseDTO address = new ViaCepResponseDTO(
                "70040-900",
                "Via N2",
                "",
                "Asa Norte",
                "Brasília",
                "DF",
                false
        );
        mockExistingDraft();
        when(viaCepService.findAddressByCep(dto.cep())).thenReturn(address);
        mockRepositorySave();

        SolicitationResponseDTO response =
                solicitationService.saveStep2(draftSolicitation.getId(), client, dto);

        assertEquals("70040900", draftSolicitation.getCep());
        assertEquals("Via N2", draftSolicitation.getStreet());
        assertEquals("Asa Norte", draftSolicitation.getNeighborhood());
        assertEquals("Brasília", draftSolicitation.getCity());
        assertEquals("DF", draftSolicitation.getState());
        assertEquals("100", draftSolicitation.getNumber());
        assertEquals("Bloco A", draftSolicitation.getComplement());
        assertEquals(2, draftSolicitation.getCurrentStep());
        assertEquals(2, response.currentStep());
        verify(viaCepService).findAddressByCep("70040-900");
        verify(solicitationRepository).save(draftSolicitation);
    }

    @Test
    @DisplayName("Deve aceitar complemento nulo no step 2")
    void shouldAcceptNullComplementAtStep2() {
        SolicitationStep2DTO dto = new SolicitationStep2DTO("70040900", "10", null);
        ViaCepResponseDTO address = new ViaCepResponseDTO(
                "70040-900", "Via N2", "", "Asa Norte", "Brasília", "DF", false
        );
        mockExistingDraft();
        when(viaCepService.findAddressByCep(dto.cep())).thenReturn(address);
        mockRepositorySave();

        solicitationService.saveStep2(draftSolicitation.getId(), client, dto);

        assertNull(draftSolicitation.getComplement());
        verify(solicitationRepository).save(draftSolicitation);
    }

    @Test
    @DisplayName("Não deve salvar o step 2 quando a consulta ao ViaCEP falhar")
    void shouldNotSaveStep2WhenViaCepFails() {
        SolicitationStep2DTO dto = new SolicitationStep2DTO("00000000", "10", null);
        mockExistingDraft();
        when(viaCepService.findAddressByCep(dto.cep()))
                .thenThrow(new IllegalArgumentException("Invalid or non-existent CEP."));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitationService.saveStep2(draftSolicitation.getId(), client, dto)
        );

        assertEquals("Invalid or non-existent CEP.", exception.getMessage());
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve preencher os dados e avançar para o step 3")
    void shouldSaveStep3() {
        SolicitationStep3DTO dto = new SolicitationStep3DTO(
                Priority.MEDIUM,
                LocalDate.now().plusDays(2),
                new BigDecimal("250.00"),
                true
        );
        mockExistingDraft();
        mockRepositorySave();

        SolicitationResponseDTO response =
                solicitationService.saveStep3(draftSolicitation.getId(), client, dto);

        assertEquals(Priority.MEDIUM, draftSolicitation.getPriority());
        assertEquals(dto.preferredDate(), draftSolicitation.getPreferredDate());
        assertEquals(new BigDecimal("250.00"), draftSolicitation.getEstimatedValue());
        assertTrue(draftSolicitation.getTermsAccepted());
        assertEquals(3, draftSolicitation.getCurrentStep());
        assertEquals(3, response.currentStep());
        verify(solicitationRepository).save(draftSolicitation);
        verifyNoInteractions(viaCepService);
    }

    @Test
    @DisplayName("Deve aceitar o valor mínimo de 100 para prioridade HIGH")
    void shouldAcceptMinimumValueForHighPriority() {
        SolicitationStep3DTO dto = new SolicitationStep3DTO(
                Priority.HIGH,
                LocalDate.now().plusDays(1),
                new BigDecimal("100.00"),
                true
        );
        mockExistingDraft();
        mockRepositorySave();

        SolicitationResponseDTO response =
                solicitationService.saveStep3(draftSolicitation.getId(), client, dto);

        assertEquals(Priority.HIGH, response.priority());
        assertEquals(new BigDecimal("100.00"), response.estimatedValue());
        verify(solicitationRepository).save(draftSolicitation);
    }

    @Test
    @DisplayName("Deve rejeitar valor inferior a 100 para prioridade HIGH")
    void shouldRejectValueBelowMinimumForHighPriority() {
        SolicitationStep3DTO dto = new SolicitationStep3DTO(
                Priority.HIGH,
                LocalDate.now().plusDays(1),
                new BigDecimal("99.99"),
                true
        );
        mockExistingDraft();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitationService.saveStep3(draftSolicitation.getId(), client, dto)
        );

        assertEquals(
                "Solicitations with HIGH priority must have an estimated value of at least 100.00.",
                exception.getMessage()
        );
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar uma solicitação inexistente")
    void shouldRejectNonexistentSolicitation() {
        UUID nonexistentId = UUID.randomUUID();
        when(solicitationRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitationService.saveStep1(nonexistentId, client, validStep1())
        );

        assertEquals("Solicitation not found with id: " + nonexistentId, exception.getMessage());
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir alteração por cliente que não seja o proprietário")
    void shouldRejectModificationByAnotherClient() {
        mockExistingDraft();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solicitationService.saveStep1(
                        draftSolicitation.getId(), anotherClient, validStep1()
                )
        );

        assertEquals(
                "Access denied. You are not the owner of this solicitation.",
                exception.getMessage()
        );
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir alteração de solicitação que não esteja em DRAFT")
    void shouldRejectModificationWhenSolicitationIsNotDraft() {
        draftSolicitation.setStatus(SolicitationStatus.SUBMITTED);
        when(solicitationRepository.findById(draftSolicitation.getId()))
                .thenReturn(Optional.of(draftSolicitation));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> solicitationService.saveStep1(
                        draftSolicitation.getId(), client, validStep1()
                )
        );

        assertEquals("Only DRAFT solicitations can be modified.", exception.getMessage());
        verify(solicitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve submeter uma solicitação completa")
    void shouldSubmitCompleteSolicitation() {
        completeDraftSolicitation();
        mockExistingDraft();
        mockRepositorySave();
        Instant beforeSubmit = Instant.now();

        SolicitationResponseDTO response =
                solicitationService.submit(draftSolicitation.getId(), client);

        Instant afterSubmit = Instant.now();
        assertEquals(SolicitationStatus.SUBMITTED, draftSolicitation.getStatus());
        assertNotNull(draftSolicitation.getSubmittedAt());
        assertFalse(draftSolicitation.getSubmittedAt().isBefore(beforeSubmit));
        assertFalse(draftSolicitation.getSubmittedAt().isAfter(afterSubmit));
        assertEquals(SolicitationStatus.SUBMITTED, response.status());
        assertEquals(draftSolicitation.getSubmittedAt(), response.submittedAt());
        verify(solicitationRepository).save(draftSolicitation);
    }

    @Test
    @DisplayName("Deve rejeitar submissão com step 1 ausente")
    void shouldRejectSubmissionWithMissingStep1() {
        completeDraftSolicitation();
        draftSolicitation.setTitle(null);
        mockExistingDraft();

        assertSubmissionConflict("Step 1 is incomplete or invalid.");
    }

    @Test
    @DisplayName("Deve rejeitar submissão com step 1 fora dos limites permitidos")
    void shouldRejectSubmissionWithInvalidStep1Length() {
        completeDraftSolicitation();
        draftSolicitation.setTitle("ab");
        mockExistingDraft();

        assertSubmissionConflict("Step 1 is incomplete or invalid.");
    }

    @Test
    @DisplayName("Deve rejeitar submissão com endereço incompleto no step 2")
    void shouldRejectSubmissionWithIncompleteStep2() {
        completeDraftSolicitation();
        draftSolicitation.setStreet(null);
        mockExistingDraft();

        assertSubmissionConflict("Step 2 (Address) is incomplete.");
    }

    @Test
    @DisplayName("Deve rejeitar submissão com step 3 incompleto")
    void shouldRejectSubmissionWithIncompleteStep3() {
        completeDraftSolicitation();
        draftSolicitation.setTermsAccepted(false);
        mockExistingDraft();

        assertSubmissionConflict("Step 3 is incomplete.");
    }

    @Test
    @DisplayName("Deve rejeitar submissão com data preferencial no passado")
    void shouldRejectSubmissionWithPastPreferredDate() {
        completeDraftSolicitation();
        draftSolicitation.setPreferredDate(LocalDate.now().minusDays(1));
        mockExistingDraft();

        assertSubmissionConflict("Preferred date cannot be in the past.");
    }

    @Test
    @DisplayName("Deve rejeitar submissão HIGH com valor inferior a 100")
    void shouldRejectHighPrioritySubmissionBelowMinimumValue() {
        completeDraftSolicitation();
        draftSolicitation.setPriority(Priority.HIGH);
        draftSolicitation.setEstimatedValue(new BigDecimal("50.00"));
        mockExistingDraft();

        assertSubmissionConflict("HIGH priority requires estimated value >= 100.");
    }

    private SolicitationStep1DTO validStep1() {
        return new SolicitationStep1DTO(
                ServiceType.MAINTENANCE,
                "Manutenção Elétrica",
                "Descrição válida da manutenção elétrica solicitada."
        );
    }

    private void mockExistingDraft() {
        when(solicitationRepository.findById(draftSolicitation.getId()))
                .thenReturn(Optional.of(draftSolicitation));
    }

    private void mockRepositorySave() {
        when(solicitationRepository.save(any(Solicitation.class))).thenAnswer(invocation -> {
            Solicitation solicitation = invocation.getArgument(0);
            if (solicitation.getId() == null) {
                solicitation.setId(UUID.randomUUID());
            }
            return solicitation;
        });
    }

    private void completeDraftSolicitation() {
        draftSolicitation.setCep("70040900");
        draftSolicitation.setStreet("Via N2");
        draftSolicitation.setNeighborhood("Asa Norte");
        draftSolicitation.setCity("Brasília");
        draftSolicitation.setState("DF");
        draftSolicitation.setNumber("100");
        draftSolicitation.setPriority(Priority.MEDIUM);
        draftSolicitation.setPreferredDate(LocalDate.now().plusDays(2));
        draftSolicitation.setEstimatedValue(new BigDecimal("250.00"));
        draftSolicitation.setTermsAccepted(true);
        draftSolicitation.setCurrentStep(3);
    }

    private void assertSubmissionConflict(String expectedMessage) {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> solicitationService.submit(draftSolicitation.getId(), client)
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(SolicitationStatus.DRAFT, draftSolicitation.getStatus());
        assertNull(draftSolicitation.getSubmittedAt());
        verify(solicitationRepository, never()).save(any());
    }
}
