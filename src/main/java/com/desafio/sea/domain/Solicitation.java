package com.desafio.sea.domain;

import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "solicitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private User clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SolicitationStatus status = SolicitationStatus.DRAFT;

    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private Integer currentStep = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Column(length = 80)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 10)
    private String cep;

    @Column(length = 255)
    private String street;

    @Column(length = 20)
    private String number;

    @Column(length = 100)
    private String complement;

    @Column(length = 100)
    private String neighborhood;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;


    // AUDITORIA
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzed_by", nullable = false)
    private User analyzedBy;

    @Column(name = "analysis_comment", length = 1000)
    private String analysisComment;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
