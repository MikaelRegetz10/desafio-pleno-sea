package com.desafio.sea.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.PageResponseDTO;
import com.desafio.sea.domain.dto.solicitation.SolicitationSearchCriteria;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.infra.elasticsearch.SolicitationDocument;
import com.desafio.sea.infra.elasticsearch.SolicitationQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitationSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SolicitationQueryBuilder queryBuilder;

    @InjectMocks
    private SolicitationSearchService searchService;

    @Mock
    private SearchHits<SolicitationDocument> searchHits;

    @Mock
    private SearchHit<SolicitationDocument> searchHit;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Analista Teste")
                .role(Role.ANALYST)
                .build();
    }

    @Test
    @DisplayName("Deve retornar página vazia se a query construída for vazia (permissão negada/sem cobertura)")
    void shouldReturnEmptyPageWhenQueryBuilderReturnsEmpty() {
        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, null, null, null
        );

        when(queryBuilder.buildQuery(criteria)).thenReturn(Optional.empty());

        PageResponseDTO<SolicitationDocument> result = searchService.search(criteria, 0, 10, "submittedAt desc");

        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertEquals(0L, result.total());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    @DisplayName("Deve executar busca no Elasticsearch e mapear os resultados com sucesso")
    void shouldExecuteSearchAndReturnMappedResults() {
        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, "teste", null, null, null, null, null, null
        );

        Query mockQuery = mock(Query.class);
        SolicitationDocument document = SolicitationDocument.builder()
                .id(UUID.randomUUID())
                .title("Instalação Solar")
                .build();

        when(queryBuilder.buildQuery(criteria)).thenReturn(Optional.of(mockQuery));
        when(searchHit.getContent()).thenReturn(document);
        when(searchHits.stream()).thenReturn(Stream.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(SolicitationDocument.class)))
                .thenReturn(searchHits);

        PageResponseDTO<SolicitationDocument> result = searchService.search(criteria, 0, 10, "submittedAt asc");

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals("Instalação Solar", result.items().get(0).getTitle());
        assertEquals(1L, result.total());
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(SolicitationDocument.class));
    }

    @Test
    @DisplayName("Deve suportar método legado com parâmetros individuais através do overload")
    void shouldSupportOverloadedSearchMethod() {
        Query mockQuery = mock(Query.class);
        when(queryBuilder.buildQuery(any(SolicitationSearchCriteria.class))).thenReturn(Optional.of(mockQuery));
        when(searchHits.stream()).thenReturn(Stream.empty());
        when(searchHits.getTotalHits()).thenReturn(0L);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(SolicitationDocument.class)))
                .thenReturn(searchHits);

        PageResponseDTO<SolicitationDocument> result = searchService.search(
                user, "query", null, null, null, "SP", null, null, 0, 10, "createdAt desc"
        );

        assertNotNull(result);
        verify(queryBuilder).buildQuery(any(SolicitationSearchCriteria.class));
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(SolicitationDocument.class));
    }
}