package dev.sey.streamfolio.catalog.dto;

import java.util.List;

public record SectionsResponse(TitleCardDto hero, List<SectionDto> sections) {
}
