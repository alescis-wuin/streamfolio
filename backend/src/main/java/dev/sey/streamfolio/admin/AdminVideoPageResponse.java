package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.catalog.dto.PageMetadataDto;
import java.util.List;

public record AdminVideoPageResponse(
    List<AdminVideoDto> items,
    PageMetadataDto pagination
) {
    public static AdminVideoPageResponse of(List<AdminVideoDto> items, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new AdminVideoPageResponse(
            items,
            new PageMetadataDto(
                page,
                size,
                totalElements,
                totalPages,
                page <= 0,
                totalPages == 0 || page >= totalPages - 1
            )
        );
    }
}
