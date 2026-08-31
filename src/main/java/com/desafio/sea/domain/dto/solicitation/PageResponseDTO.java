package com.desafio.sea.domain.dto.solicitation;

import java.util.List;

public record PageResponseDTO<T>(
        List<T> items,
        int page,
        int size,
        long total
) {}