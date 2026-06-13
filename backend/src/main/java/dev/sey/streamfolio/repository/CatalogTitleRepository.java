package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogTitleRepository extends JpaRepository<CatalogTitle, Long> {
    @EntityGraph(attributePaths = {"genres", "videos"})
    Optional<CatalogTitle> findBySlug(String slug);

    @EntityGraph(attributePaths = {"genres", "videos"})
    List<CatalogTitle> findAllByOrderByDisplayPriorityAscTitleAsc();
}
