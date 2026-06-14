package dev.sey.streamfolio.catalog;

import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.ContentType;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.SetJoin;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class CatalogTitleSpecifications {
    private CatalogTitleSpecifications() {
    }

    static Specification<CatalogTitle> filtered(String search, ContentType type, String genre) {
        Specification<CatalogTitle> spec = (root, query, builder) -> builder.conjunction();
        Specification<CatalogTitle> typeSpec = hasType(type);
        Specification<CatalogTitle> genreSpec = hasGenre(genre);
        Specification<CatalogTitle> searchSpec = matches(search);

        if (typeSpec != null) {
            spec = spec.and(typeSpec);
        }
        if (genreSpec != null) {
            spec = spec.and(genreSpec);
        }
        if (searchSpec != null) {
            spec = spec.and(searchSpec);
        }
        return spec;
    }

    private static Specification<CatalogTitle> hasType(ContentType type) {
        if (type == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    private static Specification<CatalogTitle> hasGenre(String genre) {
        String normalized = normalize(genre);
        if (normalized.isBlank()) {
            return null;
        }
        return (root, query, builder) -> {
            query.distinct(true);
            SetJoin<CatalogTitle, String> genres = root.joinSet("genres", JoinType.INNER);
            return builder.equal(builder.lower(genres), normalized);
        };
    }

    private static Specification<CatalogTitle> matches(String search) {
        String normalized = normalize(search);
        if (normalized.isBlank()) {
            return null;
        }
        String pattern = "%" + normalized + "%";
        return (root, query, builder) -> {
            query.distinct(true);
            SetJoin<CatalogTitle, String> genres = root.joinSet("genres", JoinType.LEFT);
            return builder.or(
                builder.like(builder.lower(root.get("title")), pattern),
                builder.like(builder.lower(root.get("tagline")), pattern),
                builder.like(builder.lower(root.get("synopsis")), pattern),
                builder.like(builder.lower(genres), pattern)
            );
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
