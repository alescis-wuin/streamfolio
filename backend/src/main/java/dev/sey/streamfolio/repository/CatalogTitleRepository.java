package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogTitleRepository extends JpaRepository<CatalogTitle, Long> {
    Optional<CatalogTitle> findBySlug(String slug);
    List<CatalogTitle> findAllByOrderByDisplayPriorityAscTitleAsc();
}
