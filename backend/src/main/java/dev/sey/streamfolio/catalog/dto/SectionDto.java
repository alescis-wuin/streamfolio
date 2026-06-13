package dev.sey.streamfolio.catalog.dto;

import java.util.List;

public record SectionDto(String id, String title, String description, List<TitleCardDto> items) {
}
