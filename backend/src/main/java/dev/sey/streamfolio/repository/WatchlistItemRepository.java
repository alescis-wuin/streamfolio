package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.domain.WatchlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    boolean existsByUserAndTitle(UserAccount user, CatalogTitle title);
    Optional<WatchlistItem> findByUserAndTitle(UserAccount user, CatalogTitle title);
    List<WatchlistItem> findByUser(UserAccount user);
}
