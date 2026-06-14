package dev.sey.streamfolio.catalog.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record CatalogPageResponse(
    List<TitleCardDto> items,
    PageMetadataDto pagination
) {
    public static CatalogPageResponse from(Page<TitleCardDto> page) {
        return new CatalogPageResponse(page.getContent(), PageMetadataDto.from(page));
    }
}
