package dev.sey.streamfolio.admin;

import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class AdminVideoSpecifications {
    private AdminVideoSpecifications() {
    }

    static Specification<CatalogVideo> filtered(String query, ContentType type, String genre) {
        return (root, criteriaQuery, builder) -> {
            criteriaQuery.distinct(true);
            Join<CatalogVideo, CatalogTitle> title = root.join("title", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(builder.equal(title.get("type"), type));
            }

            String normalizedGenre = normalize(genre);
            if (!normalizedGenre.isBlank()) {
                Join<CatalogTitle, String> genres = title.join("genres", JoinType.INNER);
                predicates.add(builder.equal(builder.lower(genres), normalizedGenre));
            }

            String normalizedQuery = normalize(query);
            if (!normalizedQuery.isBlank()) {
                String pattern = "%" + normalizedQuery + "%";
                Join<CatalogTitle, String> genres = title.join("genres", JoinType.LEFT);
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("videoTitle")), pattern),
                    builder.like(builder.lower(root.get("label")), pattern),
                    builder.like(builder.lower(root.get("assetFilename")), pattern),
                    builder.like(builder.lower(root.get("publicationStatus")), pattern),
                    builder.like(builder.lower(title.get("title")), pattern),
                    builder.like(builder.lower(title.get("synopsis")), pattern),
                    builder.like(builder.lower(genres), pattern)
                ));
            }

            return predicates.isEmpty()
                ? builder.conjunction()
                : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
