# Phase P0 — sécurité, catalogue et E2E

## Objectifs

Cette phase durcit le socle existant avant de poursuivre les évolutions streaming avancées.

Objectifs couverts :

- activer une protection CSRF adaptée à une PWA servie par Spring Boot ;
- conserver les sessions applicatives en cookie `HttpOnly`, sans token exposé au JavaScript ;
- limiter la console H2 au profil `dev` ;
- exposer un catalogue filtré/paginé côté backend ;
- ajouter une base de tests frontend/E2E avec Playwright ;
- adapter smoke tests, validation locale, CI et documentation.

## CSRF

Endpoint public :

```text
GET /api/csrf
```

Réponse :

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "..."
}
```

Les requêtes mutantes doivent envoyer le header indiqué par `headerName`.

Routes mutantes protégées :

- `POST /api/auth/login` ;
- `POST /api/auth/logout` ;
- `PUT /api/videos/{videoId}/progress` ;
- `POST /api/titles/{titleId}/watchlist` ;
- `DELETE /api/titles/{titleId}/watchlist`.

Côté frontend, `/csrf.js` intercepte les appels `fetch` same-origin avec méthode non sûre, récupère le jeton si nécessaire, ajoute le header CSRF et retente une fois après `403`.

## H2 console

La console H2 reste désactivée par défaut dans `application.yml`.

Elle n'est autorisée par Spring Security que si :

```yaml
spring:
  h2:
    console:
      enabled: true
```

Le profil `dev` conserve cette activation via `application-dev.yml`.

## Catalogue paginé

Endpoint :

```text
GET /api/catalog?query=botanical&type=SERIES&genre=Botanique&page=0&size=12
```

Réponse :

```json
{
  "items": [],
  "pagination": {
    "number": 0,
    "size": 12,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

Contraintes :

- `page` doit être supérieur ou égal à `0` ;
- `size` doit être compris entre `1` et `50` ;
- `type` accepte `MOVIE` ou `SERIES`.

Le filtrage est porté par `JpaSpecificationExecutor` et `CatalogTitleSpecifications` au lieu d'être appliqué sur un flux Java après chargement complet du catalogue.

## Tests ajoutés ou adaptés

Backend :

- CSRF endpoint ;
- login refusé sans CSRF ;
- mutations authentifiées refusées sans CSRF ;
- endpoints vidéo protégés ;
- H2 console refusée hors profil `dev` ;
- H2 console accessible en profil `dev` ;
- catalogue paginé/filtré ;
- pagination invalide en `400` ;
- tests streaming local adaptés au CSRF.

Frontend/E2E :

- parcours login → recherche → détail → watchlist → lecteur → logout ;
- endpoint vidéo refusé après logout ;
- catalogue API filtré et paginé ;
- mutation API refusée sans CSRF.

## Commandes

Validation complète locale :

```bash
bash scripts/validate.sh
```

E2E Playwright :

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Ou :

```bash
bash scripts/e2e.sh
```
