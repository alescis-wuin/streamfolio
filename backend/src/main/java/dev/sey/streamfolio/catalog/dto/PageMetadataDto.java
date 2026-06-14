package dev.sey.streamfolio.catalog.dto;

import org.springframework.data.domain.Page;

public record PageMetadataDto(
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static PageMetadataDto from(Page<?> page) {
        return new PageMetadataDto(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
