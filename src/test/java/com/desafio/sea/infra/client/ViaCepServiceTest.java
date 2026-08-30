package com.desafio.sea.infra.client;

import com.desafio.sea.domain.dto.solicitation.ViaCepResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViaCepServiceTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec request;
    private RestClient.ResponseSpec responseSpec;
    private ViaCepService viaCepService;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        restClient = mock(RestClient.class);
        request = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(request);
        when(request.uri(eq("/{cep}/json/"), any(Object[].class))).thenReturn(request);
        when(request.retrieve()).thenReturn(responseSpec);
        viaCepService = new ViaCepService(restClient);
    }

    @Test
    void shouldNormalizeCepAndMapAddressReturnedByViaCep() {
        when(responseSpec.body(ViaCepResponseDTO.class)).thenReturn(new ViaCepResponseDTO(
                "70040-900", "Via N2", "", "Asa Norte", "Brasília", "DF", false
        ));

        ViaCepResponseDTO response = viaCepService.findAddressByCep("70040-900");

        assertEquals("70040-900", response.cep());
        assertEquals("Via N2", response.logradouro());
        assertEquals("DF", response.uf());
    }

    @Test
    void shouldRejectAnInvalidOrNonExistentCep() {
        when(responseSpec.body(ViaCepResponseDTO.class)).thenReturn(new ViaCepResponseDTO(
                null, null, null, null, null, null, true
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> viaCepService.findAddressByCep("00000000")
        );

        assertEquals("Invalid or non-existent CEP.", exception.getMessage());
    }

    @Test
    void shouldRejectAnUnavailableCepService() {
        when(responseSpec.body(ViaCepResponseDTO.class)).thenThrow(new RuntimeException("connection refused"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> viaCepService.findAddressByCep("70040900")
        );

        assertTrue(exception.getMessage().startsWith("Failed to fetch address from CEP service:"));
    }
}
