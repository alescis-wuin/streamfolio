package dev.sey.streamfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_user_title", columnNames = {"user_id", "title_id"})
)
public class WatchlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private CatalogTitle title;

    @Column(nullable = false)
    private Instant createdAt;

    protected WatchlistItem() {
    }

    public WatchlistItem(UserAccount user, CatalogTitle title) {
        this.user = user;
        this.title = title;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public CatalogTitle getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
