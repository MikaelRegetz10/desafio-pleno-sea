package com.desafio.sea.controller;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.ViaCepResponseDTO;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.infra.client.ViaCepService;
import com.desafio.sea.repository.AuditLogRepository;
import com.desafio.sea.repository.SolicitationRepository;
import com.desafio.sea.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitationWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private SolicitationRepository solicitationRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ViaCepService viaCepService;

    private final Map<UUID, Solicitation> solicitations = new HashMap<>();
    private User client;

    @BeforeEach
    void setUp() {
        solicitations.clear();
        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente de Integração")
                .email("cliente.integration@sea.com")
                .passwordHash("not-used")
                .role(Role.CLIENT)
                .enabled(true)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        when(solicitationRepository.save(any(Solicitation.class))).thenAnswer(invocation -> {
            Solicitation solicitation = invocation.getArgument(0);
            if (solicitation.getId() == null) {
                solicitation.setId(UUID.randomUUID());
            }
            solicitations.put(solicitation.getId(), solicitation);
            return solicitation;
        });
        when(solicitationRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(solicitations.get(invocation.getArgument(0)))
        );
        when(viaCepService.findAddressByCep(anyString())).thenReturn(new ViaCepResponseDTO(
                "70040-900", "Via N2", "", "Asa Norte", "Brasília", "DF", false
        ));
    }

    @Test
    void shouldCompleteWorkflowAndBlockEditsAfterSubmit() throws Exception {
        String step1Response = mockMvc.perform(post("/solicitations/step1")
                        .with(authentication(clientAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceType":"INSTALLATION","title":"Instalação solar","description":"Descrição válida para instalação solar."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(step1Response).get("id").asText());

        mockMvc.perform(put("/solicitations/{id}/step2", id)
                        .with(authentication(clientAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"cep\":\"70040-900\",\"number\":\"100\",\"complement\":\"Bloco A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(2))
                .andExpect(jsonPath("$.state").value("DF"));

        mockMvc.perform(put("/solicitations/{id}/step3", id)
                        .with(authentication(clientAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"priority":"HIGH","preferredDate":"2099-01-01","estimatedValue":100.00,"termsAccepted":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(3));

        mockMvc.perform(post("/solicitations/{id}/submit", id)
                        .with(authentication(clientAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").value(notNullValue()));

        mockMvc.perform(put("/solicitations/{id}/step1", id)
                        .with(authentication(clientAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceType":"INSTALLATION","title":"Novo título","description":"Descrição válida para uma nova solicitação."}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Only DRAFT solicitations can be modified."));
    }

    @Test
    void shouldReturnFieldErrorsBeforeCallingTheWorkflowForInvalidStep3() throws Exception {
        mockMvc.perform(put("/solicitations/{id}/step3", UUID.randomUUID())
                        .with(authentication(clientAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"priority":null,"preferredDate":"2000-01-01","estimatedValue":-1,"termsAccepted":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.invalidFields.priority").value("Priority is required."))
                .andExpect(jsonPath("$.invalidFields.preferredDate").value("Preferred date must not be in the past."))
                .andExpect(jsonPath("$.invalidFields.estimatedValue").value("Estimated value must be greater than or equal to 0."))
                .andExpect(jsonPath("$.invalidFields.termsAccepted").value("Terms must be accepted to proceed."));
    }

    private UsernamePasswordAuthenticationToken clientAuthentication() {
        return new UsernamePasswordAuthenticationToken(client, null, client.getAuthorities());
    }
}
