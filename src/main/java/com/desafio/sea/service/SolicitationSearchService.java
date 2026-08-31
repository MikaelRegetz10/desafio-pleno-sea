package com.desafio.sea.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.PageResponseDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationSearchCriteria;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import com.desafio.sea.infra.elasticsearch.SolicitationDocument;
import com.desafio.sea.infra.elasticsearch.SolicitationQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SolicitationSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    @Autowired
    private SolicitationQueryBuilder queryBuilder;

    public PageResponseDTO<SolicitationDocument> search(SolicitationSearchCriteria criteria, int page, int size, String sort) {
        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        if (queryOpt.isEmpty()) {
            return new PageResponseDTO<>(List.of(), page, size, 0L);
        }

        Pageable pageable = parsePageable(page, size, sort);
        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(queryOpt.get())
                .withPageable(pageable)
                .build();

        SearchHits<SolicitationDocument> hits = elasticsearchOperations.search(nativeQuery, SolicitationDocument.class);

        List<SolicitationDocument> items = hits.stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageResponseDTO<>(items, page, size, hits.getTotalHits());
    }

    public PageResponseDTO<SolicitationDocument> search(
            User user,
            String q,
            List<SolicitationStatus> status,
            ServiceType serviceType,
            Priority priority,
            String state,
            Instant dateFrom,
            Instant dateTo,
            int page,
            int size,
            String sort
    ) {
        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, q, status, serviceType, priority, state, dateFrom, dateTo
        );
        return search(criteria, page, size, sort);
    }

    private Pageable parsePageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }

        String[] parts = sort.trim().split("\\s+");
        String property = parts[0];
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}