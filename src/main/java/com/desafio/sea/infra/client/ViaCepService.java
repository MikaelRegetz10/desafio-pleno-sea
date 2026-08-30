package com.desafio.sea.infra.client;

import com.desafio.sea.domain.dto.solicitation.ViaCepResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ViaCepService {

    private final RestClient restClient;

    public ViaCepService(@Value("${via-cep.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ViaCepResponseDTO findAddressByCep(String cep) {
        String cleanCep = cep.replaceAll("\\D", "");
        if (cleanCep.length() != 8) {
            throw new IllegalArgumentException("CEP must contain exactly 8 digits.");
        }

        try {
            ViaCepResponseDTO response = restClient.get()
                    .uri("/{cep}/json/", cleanCep)
                    .retrieve()
                    .body(ViaCepResponseDTO.class);

            if (response == null || Boolean.TRUE.equals(response.erro())) {
                throw new IllegalArgumentException("Invalid or non-existent CEP.");
            }

            return response;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to fetch address from CEP service: " + ex.getMessage());
        }
    }
}