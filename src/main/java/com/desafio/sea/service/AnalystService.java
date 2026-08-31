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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AnalystService {

    @Autowired
    private SolicitationRepository solicitationRepository;

    @Autowired
    private SolicitationService solicitationService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public SolicitationResponseDTO getById(UUID id, User analyst) {
        return SolicitationResponseDTO.fromEntity(findAndValidateAccess(id, analyst));
    }

    @Transactional
    public SolicitationResponseDTO start(UUID id, User analyst) {
        Solicitation solicitation = findAndValidateAccess(id, analyst);

        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED solicitations can be started for review.");
        }

        solicitation.setStatus(SolicitationStatus.IN_REVIEW);
        return SolicitationResponseDTO.fromEntity(solicitationService.saveSolicitation(solicitation));
    }

    @Transactional
    public SolicitationResponseDTO decide(UUID id, User analyst, SolicitationDecisionDTO dto) {
        Solicitation solicitation = findAndValidateAccess(id, analyst);

        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED &&
                solicitation.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new IllegalStateException("Only SUBMITTED or IN_REVIEW solicitations can be decided.");
        }

        solicitation.setStatus(dto.decision() == AnalysisDecision.APPROVE
                ? SolicitationStatus.APPROVED
                : SolicitationStatus.REJECTED);
        solicitation.setAnalysisComment(dto.comment().trim());
        solicitation.setAnalyzedBy(analyst);
        solicitation.setAnalyzedAt(Instant.now());

        return SolicitationResponseDTO.fromEntity(solicitationService.saveSolicitation(solicitation));
    }

    private Solicitation findAndValidateAccess(UUID id, User analyst) {
        Solicitation solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitation not found with id: " + id));

        if (analyst.getRole() == Role.ADMIN) {
            return solicitation;
        }
        if (analyst.getRole() != Role.ANALYST) {
            throw new AccessDeniedException("Only ANALYST or ADMIN users can access solicitations for analysis.");
        }
        if (solicitation.getState() == null || !analyst.getCoverageStates().contains(solicitation.getState())) {
            throw new AccessDeniedException("Access denied. Solicitation state is outside your coverage.");
        }

        return solicitation;
    }

    @Transactional(readOnly = true)
    public List<SolicitationResponseDTO> listAvailableForAnalyst(User analystPrincipal, SolicitationStatus status) {
        User analyst = userRepository.findById(analystPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (analyst.getRole() != Role.ANALYST && analyst.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ANALYST or ADMIN users can list solicitations for analysis.");
        }

        List<Solicitation> solicitations;

        if (analyst.getRole() == Role.ADMIN) {
            solicitations = solicitationRepository.findByOptionalStatus(status);
        } else {
            Set<String> coverageStates = analyst.getCoverageStates();
            if (coverageStates == null || coverageStates.isEmpty()) {
                return List.of();
            }
            solicitations = solicitationRepository.findByStateInAndOptionalStatus(coverageStates, status);
        }

        return solicitations.stream()
                .map(SolicitationResponseDTO::fromEntity)
                .toList();
    }
}
