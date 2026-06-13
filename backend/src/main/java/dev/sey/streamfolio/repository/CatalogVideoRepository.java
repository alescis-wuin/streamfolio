package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogVideo;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogVideoRepository extends JpaRepository<CatalogVideo, Long> {
    @Override
    @EntityGraph(attributePaths = {"title", "title.genres"})
    Optional<CatalogVideo> findById(Long id);
}
