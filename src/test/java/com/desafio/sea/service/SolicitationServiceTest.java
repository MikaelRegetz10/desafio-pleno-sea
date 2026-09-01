package com.desafio.sea.service;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.*;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.infra.client.ViaCepService;
import com.desafio.sea.infra.elasticsearch.SolicitationIndexerService;
import com.desafio.sea.repository.SolicitationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private ViaCepService viaCepService;

    @Mock
    private SolicitationIndexerService indexerService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private SolicitationService solicitationService;

    private User client;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente Teste")
                .email("cliente@sea.com")
                .role(Role.CLIENT)
                .build();

        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        lenient().when(solicitationRepository.save(any(Solicitation.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldCreateSolicitationAtStep1() {
        SolicitationStep1DTO dto = new SolicitationStep1DTO(ServiceType.INSTALLATION, "Instalação Solar", "Descrição detalhada da solicitação com mais de vinte caracteres.");

        SolicitationResponseDTO response = solicitationService.saveStep1(null, client, dto);

        assertNotNull(response);
        assertEquals(SolicitationStatus.DRAFT, response.status());
        assertEquals(1, response.currentStep());
        verify(indexerService).index(any(Solicitation.class));
    }

    @Test
    void shouldSaveStep2WithAddressReturnedByViaCep() {
        UUID id = UUID.randomUUID();
        Solicitation draft = Solicitation.builder().id(id).client(client).status(SolicitationStatus.DRAFT).currentStep(1).build();
        when(solicitationRepository.findById(id)).thenReturn(Optional.of(draft));

        ViaCepResponseDTO viaCep = new ViaCepResponseDTO("70040-900", "Via N2", "", "Asa Norte", "Brasília", "DF", false);
        when(viaCepService.findAddressByCep("70040-900")).thenReturn(viaCep);

        SolicitationStep2DTO dto = new SolicitationStep2DTO("70040-900", "100", "Bloco A");
        SolicitationResponseDTO response = solicitationService.saveStep2(id, client, dto);

        assertNotNull(response);
        assertEquals("DF", response.state());
        assertEquals(2, response.currentStep());
        verify(indexerService).index(any(Solicitation.class));
    }

    @Test
    void shouldSaveStep3() {
        UUID id = UUID.randomUUID();
        Solicitation draft = Solicitation.builder().id(id).client(client).status(SolicitationStatus.DRAFT).currentStep(2).build();
        when(solicitationRepository.findById(id)).thenReturn(Optional.of(draft));

        SolicitationStep3DTO dto = new SolicitationStep3DTO(Priority.MEDIUM, LocalDate.now().plusDays(5), new BigDecimal("250.00"), true);
        SolicitationResponseDTO response = solicitationService.saveStep3(id, client, dto);

        assertNotNull(response);
        assertEquals(Priority.MEDIUM, response.priority());
        assertEquals(3, response.currentStep());
        verify(indexerService).index(any(Solicitation.class));
    }
}