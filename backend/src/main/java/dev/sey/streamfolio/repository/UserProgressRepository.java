package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.UserProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    @EntityGraph(attributePaths = {"video"})
    Optional<UserProgress> findByUserAndVideo(UserAccount user, CatalogVideo video);

    @EntityGraph(attributePaths = {"video"})
    List<UserProgress> findByUser(UserAccount user);
}
