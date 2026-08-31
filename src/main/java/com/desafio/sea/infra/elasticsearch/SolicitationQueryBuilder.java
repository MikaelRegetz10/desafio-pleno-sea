package com.desafio.sea.infra.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.desafio.sea.domain.User;
import com.desafio.sea.domain.dto.solicitation.SolicitationSearchCriteria;
import com.desafio.sea.domain.enums.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class SolicitationQueryBuilder {

    public Optional<Query> buildQuery(SolicitationSearchCriteria criteria) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (!applyUserRoleFilter(boolQuery, criteria.user(), criteria.getNormalizedState())) {
            return Optional.empty();
        }

        applyTextSearch(boolQuery, criteria.queryText());
        applyStatusFilter(boolQuery, criteria.status());
        applyServiceTypeFilter(boolQuery, criteria.serviceType());
        applyPriorityFilter(boolQuery, criteria.priority());
        applyDateRangeFilter(boolQuery, criteria.dateFrom(), criteria.dateTo());

        return Optional.of(Query.of(q -> q.bool(boolQuery.build())));
    }

    private boolean applyUserRoleFilter(BoolQuery.Builder boolQuery, User user, String requestedState) {
        if (user.getRole() != Role.ANALYST) {
            if (requestedState != null && !requestedState.isBlank()) {
                boolQuery.filter(f -> f.term(t -> t.field("state").value(requestedState.trim().toUpperCase())));
            }
            return true;
        }

        Set<String> coverage = user.getCoverageStates();
        if (coverage == null || coverage.isEmpty()) {
            return false;
        }

        List<String> allowedStates = coverage.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        if (allowedStates.isEmpty()) {
            return false;
        }

        if (requestedState != null && !requestedState.isBlank()) {
            String targetState = requestedState.trim().toUpperCase();
            if (allowedStates.contains(targetState)) {
                boolQuery.filter(f -> f.term(t -> t.field("state").value(targetState)));
                return true;
            }
        }

        if (allowedStates.size() == 1) {
            boolQuery.filter(f -> f.term(t -> t.field("state").value(allowedStates.get(0))));
        } else {
            List<FieldValue> fieldValues = allowedStates.stream()
                    .map(FieldValue::of)
                    .toList();
            boolQuery.filter(f -> f.terms(t -> t.field("state").terms(v -> v.value(fieldValues))));
        }

        return true;
    }

    private void applyTextSearch(BoolQuery.Builder boolQuery, String queryText) {
        if (queryText != null && !queryText.isBlank()) {
            boolQuery.must(m -> m.multiMatch(mm -> mm
                    .fields("title", "description")
                    .query(queryText.trim())
            ));
        }
    }

    private void applyStatusFilter(BoolQuery.Builder boolQuery, List<?> statusList) {
        if (statusList != null && !statusList.isEmpty()) {
            List<FieldValue> values = statusList.stream()
                    .map(Object::toString)
                    .map(FieldValue::of)
                    .toList();

            if (values.size() == 1) {
                boolQuery.filter(f -> f.term(t -> t.field("status").value(values.get(0).stringValue())));
            } else {
                boolQuery.filter(f -> f.terms(t -> t.field("status").terms(v -> v.value(values))));
            }
        }
    }

    private void applyServiceTypeFilter(BoolQuery.Builder boolQuery, Object serviceType) {
        if (serviceType != null) {
            boolQuery.filter(f -> f.term(t -> t.field("serviceType").value(serviceType.toString())));
        }
    }

    private void applyPriorityFilter(BoolQuery.Builder boolQuery, Object priority) {
        if (priority != null) {
            boolQuery.filter(f -> f.term(t -> t.field("priority").value(priority.toString())));
        }
    }

    private void applyDateRangeFilter(BoolQuery.Builder boolQuery, Object dateFrom, Object dateTo) {
        if (dateFrom != null || dateTo != null) {
            boolQuery.filter(Query.of(q -> q.range(r -> r.date(d -> {
                d.field("submittedAt");
                if (dateFrom != null) d.gte(dateFrom.toString());
                if (dateTo != null) d.lte(dateTo.toString());
                return d;
            }))));
        }
    }
}