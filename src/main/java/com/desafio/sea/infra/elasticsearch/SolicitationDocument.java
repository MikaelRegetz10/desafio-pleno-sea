package com.desafio.sea.infra.elasticsearch;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "solicitations")
public class SolicitationDocument {

    @Id
    private UUID id;

    @Field(type = FieldType.Keyword)
    private UUID clientId;

    @Field(type = FieldType.Keyword)
    private SolicitationStatus status;

    @Field(type = FieldType.Keyword)
    private ServiceType serviceType;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private Priority priority;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant submittedAt;

    public static SolicitationDocument fromEntity(Solicitation entity) {
        return SolicitationDocument.builder()
                .id(entity.getId())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .status(entity.getStatus())
                .serviceType(entity.getServiceType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .state(entity.getState())
                .city(entity.getCity())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}