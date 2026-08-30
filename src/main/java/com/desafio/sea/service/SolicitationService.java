package com.desafio.sea.service;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.*;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.infra.client.ViaCepService;
import com.desafio.sea.repository.SolicitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class SolicitationService {

    @Autowired
    private SolicitationRepository solicitationRepository;
    @Autowired
    private ViaCepService viaCepService;

    @Transactional
    public SolicitationResponseDTO saveStep1(UUID id, User client, SolicitationStep1DTO dto) {
        Solicitation solicitation;

        if (id == null) {
            solicitation = Solicitation.builder()
                    .client(client)
                    .status(SolicitationStatus.DRAFT)
                    .currentStep(1)
                    .build();
        } else {
            solicitation = findAndValidateDraftOwnership(id, client);
        }

        solicitation.setServiceType(dto.serviceType());
        solicitation.setTitle(dto.title().trim());
        solicitation.setDescription(dto.description().trim());

        if (solicitation.getCurrentStep() < 1) {
            solicitation.setCurrentStep(1);
        }

        return SolicitationResponseDTO.fromEntity(solicitationRepository.save(solicitation));
    }

    @Transactional
    public SolicitationResponseDTO saveStep2(UUID id, User client, SolicitationStep2DTO dto) {
        Solicitation solicitation = findAndValidateDraftOwnership(id, client);

        ViaCepResponseDTO address = viaCepService.findAddressByCep(dto.cep());

        solicitation.setCep(address.cep().replaceAll("\\D", ""));
        solicitation.setStreet(address.logradouro());
        solicitation.setNeighborhood(address.bairro());
        solicitation.setCity(address.localidade());
        solicitation.setState(address.uf());
        solicitation.setNumber(dto.number().trim());
        solicitation.setComplement(dto.complement() != null ? dto.complement().trim() : null);

        if (solicitation.getCurrentStep() < 2) {
            solicitation.setCurrentStep(2);
        }

        return SolicitationResponseDTO.fromEntity(solicitationRepository.save(solicitation));
    }

    @Transactional
    public SolicitationResponseDTO saveStep3(UUID id, User client, SolicitationStep3DTO dto) {
        Solicitation solicitation = findAndValidateDraftOwnership(id, client);

        if (dto.priority() == Priority.HIGH && dto.estimatedValue().compareTo(new BigDecimal("100")) < 0) {
            throw new IllegalArgumentException("Solicitations with HIGH priority must have an estimated value of at least 100.00.");
        }

        solicitation.setPriority(dto.priority());
        solicitation.setPreferredDate(dto.preferredDate());
        solicitation.setEstimatedValue(dto.estimatedValue());
        solicitation.setTermsAccepted(dto.termsAccepted());

        solicitation.setCurrentStep(3);

        return SolicitationResponseDTO.fromEntity(solicitationRepository.save(solicitation));
    }

    @Transactional
    public SolicitationResponseDTO submit(UUID id, User client) {
        Solicitation solicitation = findAndValidateDraftOwnership(id, client);

        validateStep1Completeness(solicitation);
        validateStep2Completeness(solicitation);
        validateStep3Completeness(solicitation);

        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        solicitation.setSubmittedAt(Instant.now());

        return SolicitationResponseDTO.fromEntity(solicitationRepository.save(solicitation));
    }

    private Solicitation findAndValidateDraftOwnership(UUID id, User client) {
        Solicitation solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitation not found with id: " + id));

        if (!solicitation.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Access denied. You are not the owner of this solicitation.");
        }

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT solicitations can be modified.");
        }

        return solicitation;
    }

    private void validateStep1Completeness(Solicitation s) {
        if (s.getServiceType() == null || s.getTitle() == null || s.getDescription() == null ||
                s.getTitle().length() < 3 || s.getTitle().length() > 80 ||
                s.getDescription().length() < 20 || s.getDescription().length() > 1000) {
            throw new IllegalStateException("Step 1 is incomplete or invalid.");
        }
    }

    private void validateStep2Completeness(Solicitation s) {
        if (s.getCep() == null || s.getStreet() == null || s.getNeighborhood() == null ||
                s.getCity() == null || s.getState() == null || s.getNumber() == null) {
            throw new IllegalStateException("Step 2 (Address) is incomplete.");
        }
    }

    private void validateStep3Completeness(Solicitation s) {
        if (s.getPriority() == null || s.getPreferredDate() == null || s.getEstimatedValue() == null ||
                Boolean.FALSE.equals(s.getTermsAccepted())) {
            throw new IllegalStateException("Step 3 is incomplete.");
        }
        if (s.getPreferredDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Preferred date cannot be in the past.");
        }
        if (s.getPriority() == Priority.HIGH && s.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            throw new IllegalStateException("HIGH priority requires estimated value >= 100.");
        }
    }
}