package dev.sey.streamfolio.seed;

import dev.sey.streamfolio.auth.AuthService;
import dev.sey.streamfolio.domain.CatalogTitle;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.domain.ContentType;
import dev.sey.streamfolio.domain.UserAccount;
import dev.sey.streamfolio.repository.CatalogTitleRepository;
import dev.sey.streamfolio.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataLoader implements CommandLineRunner {
    private final CatalogTitleRepository titles;
    private final UserAccountRepository users;

    public DemoDataLoader(CatalogTitleRepository titles, UserAccountRepository users) {
        this.titles = titles;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUser();
        seedCatalog();
    }

    private void seedUser() {
        users.findByEmail("alexis@example.dev").orElseGet(() -> users.save(
            new UserAccount("alexis@example.dev", "Alexis", AuthService.hashPassword("demo1234"))
        ));
    }

    private void seedCatalog() {
        saveOrRefresh(new CatalogTitle(
            "aurora-drift",
            "Aurora Drift",
            ContentType.MOVIE,
            2026,
            "10+",
            1,
            "Un court métrage abstrait pour tester le lecteur.",
            "Une exploration visuelle minimaliste, conçue comme contenu neutre pour démontrer le streaming progressif, les sous-titres et la reprise de lecture.",
            posterPath("aurora-drift"),
            posterPath("aurora-drift"),
            1
        ).addGenre("Science-fiction").addGenre("Atmosphérique").addGenre("Court")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "botanical-cities",
            "Botanical Cities",
            ContentType.SERIES,
            2025,
            "Tous publics",
            2,
            "Une série documentaire fictive sur la botanique urbaine.",
            "Des capsules visuelles courtes autour de villes végétales imaginaires. La série sert à montrer la gestion des épisodes et la progression par vidéo.",
            posterPath("botanical-cities"),
            posterPath("botanical-cities"),
            2
        ).addGenre("Documentaire").addGenre("Botanique").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Canopées urbaines", 12, "botanical-cities-01.mp4", "botanical-cities-01.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Serres nocturnes", 12, "botanical-cities-02.mp4", "botanical-cities-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "silent-protocol",
            "Silent Protocol",
            ContentType.MOVIE,
            2024,
            "12+",
            1,
            "Un thriller tech très court, pensé pour une UI épurée.",
            "Un protocole se déclenche dans une interface sans bruit. Le film est une donnée de démonstration, libre de tout contenu protégé.",
            posterPath("silent-protocol"),
            posterPath("silent-protocol"),
            3
        ).addGenre("Thriller").addGenre("Technologie").addGenre("Minimaliste")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "silent-protocol.mp4", "silent-protocol.vtt")));

        saveOrRefresh(new CatalogTitle(
            "kitchen-orbit",
            "Kitchen Orbit",
            ContentType.SERIES,
            2026,
            "Tous publics",
            2,
            "Micro-série culinaire expérimentale.",
            "Une capsule visuelle qui montre qu'une plateforme de streaming peut mélanger films, séries, documentaires et formats courts.",
            posterPath("kitchen-orbit"),
            posterPath("kitchen-orbit"),
            4
        ).addGenre("Cuisine").addGenre("Créatif").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Émulsion orbitale", 12, "kitchen-orbit-01.mp4", "kitchen-orbit-01.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Textures calmes", 12, "kitchen-orbit-02.mp4", "kitchen-orbit-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "neon-orchard",
            "Neon Orchard",
            ContentType.MOVIE,
            2026,
            "10+",
            1,
            "Une dérive nocturne entre science-fiction douce et paysage végétal.",
            "Titre de démonstration ajouté pour densifier les rails de contenu et donner une meilleure impression de catalogue complet sur écran large.",
            posterPath("neon-orchard"),
            posterPath("neon-orchard"),
            5
        ).addGenre("Science-fiction").addGenre("Botanique").addGenre("Original")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "glass-archive",
            "Glass Archive",
            ContentType.SERIES,
            2025,
            "12+",
            2,
            "Une enquête en deux épisodes dans une mémoire visuelle fragmentée.",
            "Série fictive courte pour illustrer les pages détail, la reprise de lecture par épisode et les recommandations segmentées.",
            posterPath("glass-archive"),
            posterPath("glass-archive"),
            6
        ).addGenre("Mystère").addGenre("Technologie").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Fragments", 12, "silent-protocol.mp4", "silent-protocol.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Rémanence", 12, "botanical-cities-02.mp4", "botanical-cities-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "carbon-tide",
            "Carbon Tide",
            ContentType.MOVIE,
            2024,
            "10+",
            1,
            "Un court format contemplatif sur une mer synthétique.",
            "Film de démonstration pour varier les affiches, tester les filtres par genre et remplir les rangées horizontales façon plateforme de streaming.",
            posterPath("carbon-tide"),
            posterPath("carbon-tide"),
            7
        ).addGenre("Atmosphérique").addGenre("Minimaliste").addGenre("Original")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "quiet-circuit",
            "Quiet Circuit",
            ContentType.SERIES,
            2026,
            "12+",
            2,
            "Une mini-série tech sobre, structurée autour de signaux faibles.",
            "Deux épisodes fictifs pour démontrer une expérience de détail claire, avec progression indépendante par vidéo et raccourcis de lecture.",
            posterPath("quiet-circuit"),
            posterPath("quiet-circuit"),
            8
        ).addGenre("Technologie").addGenre("Thriller").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Handshake", 12, "silent-protocol.mp4", "silent-protocol.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Fallback", 12, "kitchen-orbit-01.mp4", "kitchen-orbit-01.vtt")));

        saveOrRefresh(new CatalogTitle(
            "nomad-frames",
            "Nomad Frames",
            ContentType.MOVIE,
            2025,
            "Tous publics",
            1,
            "Un carnet de voyage abstrait pour tester une grille riche.",
            "Court métrage fictif orienté portfolio, utilisé pour démontrer la recherche, les métadonnées et une navigation visuelle dense mais lisible.",
            posterPath("nomad-frames"),
            posterPath("nomad-frames"),
            9
        ).addGenre("Documentaire").addGenre("Créatif").addGenre("Court")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "botanical-cities-01.mp4", "botanical-cities-01.vtt")));

        saveOrRefresh(new CatalogTitle(
            "signal-garden",
            "Signal Garden",
            ContentType.SERIES,
            2026,
            "Tous publics",
            2,
            "Une série calme où des capteurs racontent un jardin imaginaire.",
            "Donnée de démonstration pensée pour valoriser les genres botanique, documentaire et technologie dans un catalogue plus réaliste.",
            posterPath("signal-garden"),
            posterPath("signal-garden"),
            10
        ).addGenre("Botanique").addGenre("Documentaire").addGenre("Technologie")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Germination", 12, "botanical-cities-01.mp4", "botanical-cities-01.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Telemetry", 12, "botanical-cities-02.mp4", "botanical-cities-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "lunar-kernel",
            "Lunar Kernel",
            ContentType.MOVIE,
            2026,
            "10+",
            1,
            "Un film court entre architecture lunaire et noyau logiciel.",
            "Titre de démonstration conçu pour densifier les carrousels et montrer une direction artistique abstraite sans texte incrusté dans les miniatures.",
            posterPath("lunar-kernel"),
            posterPath("lunar-kernel"),
            11
        ).addGenre("Science-fiction").addGenre("Technologie").addGenre("Court")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "velvet-latency",
            "Velvet Latency",
            ContentType.SERIES,
            2025,
            "12+",
            2,
            "Une série tech sur les délais invisibles d'un réseau fermé.",
            "Deux épisodes fictifs pensés pour remplir les rails séries et tester l'aperçu centré au clic avec des métadonnées plus complètes.",
            posterPath("velvet-latency"),
            posterPath("velvet-latency"),
            12
        ).addGenre("Thriller").addGenre("Technologie").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Ping", 12, "silent-protocol.mp4", "silent-protocol.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Timeout", 12, "kitchen-orbit-02.mp4", "kitchen-orbit-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "moss-engine",
            "Moss Engine",
            ContentType.MOVIE,
            2024,
            "Tous publics",
            1,
            "Un moteur végétal abstrait, entre botanique et mécanique douce.",
            "Court métrage de démonstration destiné à varier les genres et à éviter des sections visuellement trop pauvres sur écran large.",
            posterPath("moss-engine"),
            posterPath("moss-engine"),
            13
        ).addGenre("Botanique").addGenre("Atmosphérique").addGenre("Original")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "botanical-cities-01.mp4", "botanical-cities-01.vtt")));

        saveOrRefresh(new CatalogTitle(
            "chrome-harbor",
            "Chrome Harbor",
            ContentType.SERIES,
            2026,
            "10+",
            2,
            "Une série courte dans un port industriel silencieux.",
            "Série fictive ajoutée pour augmenter la densité du catalogue et donner aux carrousels un comportement plus réaliste avec débordement horizontal.",
            posterPath("chrome-harbor"),
            posterPath("chrome-harbor"),
            14
        ).addGenre("Mystère").addGenre("Minimaliste").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Dock", 12, "silent-protocol.mp4", "silent-protocol.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Fog", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "ember-archive",
            "Ember Archive",
            ContentType.MOVIE,
            2025,
            "12+",
            1,
            "Une archive visuelle se consume dans une interface calme.",
            "Film fictif pensé pour enrichir les sections nouveautés et films, avec une miniature purement graphique et sans typographie intégrée.",
            posterPath("ember-archive"),
            posterPath("ember-archive"),
            15
        ).addGenre("Mystère").addGenre("Atmosphérique").addGenre("Court")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "silent-protocol.mp4", "silent-protocol.vtt")));

        saveOrRefresh(new CatalogTitle(
            "orbit-bakery",
            "Orbit Bakery",
            ContentType.SERIES,
            2026,
            "Tous publics",
            2,
            "Une micro-série culinaire autour de gestes orbitaux.",
            "Deux capsules fictives pour renforcer la catégorie séries et diversifier les genres sans ajouter de dépendances médias lourdes.",
            posterPath("orbit-bakery"),
            posterPath("orbit-bakery"),
            16
        ).addGenre("Cuisine").addGenre("Créatif").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Pâte lente", 12, "kitchen-orbit-01.mp4", "kitchen-orbit-01.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Levain orbital", 12, "kitchen-orbit-02.mp4", "kitchen-orbit-02.vtt")));

        saveOrRefresh(new CatalogTitle(
            "tundra-signal",
            "Tundra Signal",
            ContentType.MOVIE,
            2024,
            "10+",
            1,
            "Un signal froid traverse un paysage synthétique.",
            "Titre de démonstration supplémentaire pour augmenter le volume perçu du catalogue et tester la recherche par genre.",
            posterPath("tundra-signal"),
            posterPath("tundra-signal"),
            17
        ).addGenre("Science-fiction").addGenre("Minimaliste").addGenre("Original")
            .addVideo(new CatalogVideo(1, 1, "Film", "Lecture complète", 12, "aurora-drift.mp4", "aurora-drift.vtt")));

        saveOrRefresh(new CatalogTitle(
            "pixel-greenhouse",
            "Pixel Greenhouse",
            ContentType.SERIES,
            2025,
            "Tous publics",
            2,
            "Une serre numérique observée par fragments.",
            "Série fictive ajoutée pour donner plus de contenu aux sections documentaire, botanique et technologie sur grands écrans.",
            posterPath("pixel-greenhouse"),
            posterPath("pixel-greenhouse"),
            18
        ).addGenre("Botanique").addGenre("Documentaire").addGenre("Série")
            .addVideo(new CatalogVideo(1, 1, "S1:E1", "Substrat", 12, "botanical-cities-01.mp4", "botanical-cities-01.vtt"))
            .addVideo(new CatalogVideo(1, 2, "S1:E2", "Photosynthèse", 12, "botanical-cities-02.mp4", "botanical-cities-02.vtt")));
    }

    private static final String POSTER_VERSION = "v6";

    private String posterPath(String slug) {
        return "/assets/posters-clean/" + slug + ".svg?" + POSTER_VERSION;
    }

    private void saveOrRefresh(CatalogTitle title) {
        titles.findBySlug(title.getSlug()).ifPresentOrElse(
            existing -> existing.refreshFrom(title),
            () -> titles.save(title)
        );
    }
}
