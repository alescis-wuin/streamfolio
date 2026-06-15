package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CatalogVideoRepository extends JpaRepository<CatalogVideo, Long> {
    @Override
    @EntityGraph(attributePaths = {"title", "title.genres"})
    Optional<CatalogVideo> findById(Long id);

    @EntityGraph(attributePaths = {"title", "title.genres"})
    @Query("select video from CatalogVideo video")
    List<CatalogVideo> findAllWithTitleGraph();

    long countByTitle(CatalogTitle title);

    @EntityGraph(attributePaths = {"title", "title.genres"})
    List<CatalogVideo> findByTitleOrderBySeasonNumberAscEpisodeNumberAscIdAsc(CatalogTitle title);
}
