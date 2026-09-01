package com.desafio.sea.infra.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationSearchCriteria;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.Role;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitationQueryBuilderTest {

    private SolicitationQueryBuilder queryBuilder;

    @Mock
    private User user;

    @BeforeEach
    void setUp() {
        queryBuilder = new SolicitationQueryBuilder();
    }

    @Test
    @DisplayName("Deve retornar Optional.empty quando ANALYST possui cobertura nula")
    void shouldReturnEmptyWhenAnalystHasNullCoverage() {
        when(user.getRole()).thenReturn(Role.ANALYST);
        when(user.getCoverageStates()).thenReturn(null);

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, null, null, null
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar Optional.empty quando ANALYST possui cobertura vazia")
    void shouldReturnEmptyWhenAnalystHasEmptyCoverage() {
        when(user.getRole()).thenReturn(Role.ANALYST);
        when(user.getCoverageStates()).thenReturn(Collections.emptySet());

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, null, null, null
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isEmpty());
    }

    @Test
    @DisplayName("Deve realizar fallback para todas as UFs de cobertura quando ANALYST solicita estado fora de sua cobertura")
    void shouldFallbackToCoverageStatesWhenAnalystRequestsStateOutsideCoverage() {
        when(user.getRole()).thenReturn(Role.ANALYST);
        when(user.getCoverageStates()).thenReturn(Set.of("SP", "RJ"));

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, "MG", null, null
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isPresent());
        Query query = queryOpt.get();
        assertTrue(query.isBool());
        assertFalse(query.bool().filter().isEmpty());
    }

    @Test
    @DisplayName("Deve construir query com sucesso quando ANALYST solicita estado dentro da sua cobertura")
    void shouldBuildQueryWhenAnalystRequestsAllowedState() {
        when(user.getRole()).thenReturn(Role.ANALYST);
        when(user.getCoverageStates()).thenReturn(Set.of("SP", "RJ"));

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, "sp", null, null
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isPresent());
        Query query = queryOpt.get();
        assertTrue(query.isBool());
        assertFalse(query.bool().filter().isEmpty());
    }

    @Test
    @DisplayName("Deve filtrar por todos os estados de cobertura quando ANALYST não especifica estado")
    void shouldFilterByAllCoverageStatesWhenAnalystOmitsState() {
        when(user.getRole()).thenReturn(Role.ANALYST);
        when(user.getCoverageStates()).thenReturn(Set.of("SP", "RJ"));

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user, null, null, null, null, null, null, null
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isPresent());
        Query query = queryOpt.get();
        assertTrue(query.isBool());
        assertFalse(query.bool().filter().isEmpty());
    }

    @Test
    @DisplayName("Deve aplicar todos os filtros configurados no critérios de busca para usuário ADMIN")
    void shouldApplyAllFiltersForAdminUser() {
        when(user.getRole()).thenReturn(Role.ADMIN);

        SolicitationSearchCriteria criteria = new SolicitationSearchCriteria(
                user,
                "manutenção",
                List.of(SolicitationStatus.SUBMITTED, SolicitationStatus.IN_REVIEW),
                ServiceType.MAINTENANCE,
                Priority.HIGH,
                "DF",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")
        );

        Optional<Query> queryOpt = queryBuilder.buildQuery(criteria);

        assertTrue(queryOpt.isPresent());
        Query query = queryOpt.get();
        assertTrue(query.isBool());
        assertFalse(query.bool().must().isEmpty());
        assertFalse(query.bool().filter().isEmpty());
    }
}