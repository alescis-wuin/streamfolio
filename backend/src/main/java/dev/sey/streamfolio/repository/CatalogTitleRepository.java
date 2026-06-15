package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CatalogTitleRepository extends JpaRepository<CatalogTitle, Long>, JpaSpecificationExecutor<CatalogTitle>, CatalogTitleGraphRepository {
    @EntityGraph(attributePaths = {"genres", "videos"})
    Optional<CatalogTitle> findBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = {"genres", "videos"})
    Optional<CatalogTitle> findById(Long id);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"genres", "videos"})
    List<CatalogTitle> findAllByOrderByDisplayPriorityAscTitleAsc();
}
