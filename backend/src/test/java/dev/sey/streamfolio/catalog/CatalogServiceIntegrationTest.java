package dev.sey.streamfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sey.streamfolio.catalog.dto.ProgressUpdateRequest;
import dev.sey.streamfolio.catalog.dto.TitleCardDto;
import dev.sey.streamfolio.common.BadRequestException;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.repository.UserAccountRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CatalogServiceIntegrationTest {
    @Autowired
    private CatalogService catalogService;

    @Autowired
    private UserAccountRepository users;

    @Test
    void anonymousCatalogIsReadableWithoutPersonalState() {
        List<TitleCardDto> catalog = catalogService.catalog(null, null, null, null);

        assertThat(catalog).isNotEmpty();
        assertThat(catalog).allSatisfy(card -> {
            assertThat(card.inWatchlist()).isFalse();
            assertThat(card.progress().positionSeconds()).isZero();
            assertThat(card.nextVideoId()).isNotNull();
        });
    }

    @Test
    void filtersCatalogByTypeGenreAndQuery() {
        List<TitleCardDto> series = catalogService.catalog("botanical", "SERIES", "Botanique", null);

        assertThat(series).isNotEmpty();
        assertThat(series).allSatisfy(card -> {
            assertThat(card.type()).isEqualTo(ContentType.SERIES);
            assertThat(card.genres()).contains("Botanique");
            assertThat(card.title().toLowerCase()).contains("botanical");
        });
    }

    @Test
    void invalidTypeFailsFast() {
        assertThatThrownBy(() -> catalogService.catalog(null, "DOCUMENTARY", null, null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Type invalide");
    }

    @Test
    void watchlistIsReflectedInCatalogCards() {
        UserAccount user = demoUser();
        TitleCardDto target = firstMovie();

        catalogService.addToWatchlist(target.id(), user);
        assertThat(findCard(target.id(), user).inWatchlist()).isTrue();

        catalogService.removeFromWatchlist(target.id(), user);
        assertThat(findCard(target.id(), user).inWatchlist()).isFalse();
    }

    @Test
    void progressIsAggregatedInCatalogCards() {
        UserAccount user = demoUser();
        TitleCardDto target = firstMovie();

        catalogService.updateProgress(target.nextVideoId(), new ProgressUpdateRequest(6, 12), user);

        TitleCardDto updated = findCard(target.id(), user);
        assertThat(updated.progress().positionSeconds()).isEqualTo(6);
        assertThat(updated.progress().durationSeconds()).isEqualTo(12);
        assertThat(updated.progress().percent()).isEqualTo(50.0);
        assertThat(updated.progress().completed()).isFalse();
    }

    private UserAccount demoUser() {
        return users.findByEmail("alexis@example.dev").orElseThrow();
    }

    private TitleCardDto firstMovie() {
        return catalogService.catalog(null, "MOVIE", null, null).stream()
            .findFirst()
            .orElseThrow();
    }

    private TitleCardDto findCard(Long titleId, UserAccount user) {
        return catalogService.catalog(null, null, null, user).stream()
            .filter(card -> card.id().equals(titleId))
            .findFirst()
            .orElseThrow();
    }
}
