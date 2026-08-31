package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.User;
import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import com.desafio.sea.domain.enums.SolicitationStatus;

import java.time.Instant;
import java.util.List;

public record SolicitationSearchCriteria(
        User user,
        String queryText,
        List<SolicitationStatus> status,
        ServiceType serviceType,
        Priority priority,
        String state,
        Instant dateFrom,
        Instant dateTo
) {
    public boolean hasQueryText() {
        return queryText != null && !queryText.isBlank();
    }

    public boolean hasStatus() {
        return status != null && !status.isEmpty();
    }

    public boolean hasState() {
        return state != null && !state.isBlank();
    }

    public boolean hasDateRange() {
        return dateFrom != null || dateTo != null;
    }

    public String getNormalizedState() {
        return hasState() ? state.trim().toUpperCase() : null;
    }
}