package com.king.paynith.common.response;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        Metadata metadata
) {
    public record Metadata (
            int page,
            int size,
            long totalItems,
            int totalPages,
            boolean isFirst,
            boolean isLast
    ){}

    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                new Metadata(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast()
                )
        );
    }
}