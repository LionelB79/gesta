# Gesta

Plateforme modulaire de gestion — réservation de salles, matériel, documents, référentiels — construite comme un **monolithe modulaire** Spring Boot + React.

> **Démo en ligne** : _URL à venir_ — bouton « Connexion démo » sur la page d'accueil (aucune inscription nécessaire).

## Vue d'ensemble

Gesta est le portail (authentification + accueil) qui héberge des modules métier aux frontières strictes :

| Module      | Rôle                                                        | Statut  |
|-------------|-------------------------------------------------------------|---------|
| `gesta`     | Portail : auth, accueil, assemblage de l'application        | v1      |
| `resa`      | Réservation de salles                                       | v1      |
| `squelette` | Socle commun : exceptions, audit, génériques, constantes    | v1      |
| `materio`   | Gestion du matériel                                         | à venir |
| `odito`     | Édition de modèles de documents (variables stat./dyn.)      | à venir |
| `staff`     | Consultation de données via requêtes SQL cataloguées        | à venir |
| `ref`       | Référentiels partagés (items, liens)                        | à venir |
| `stocko`    | Stockage de fichiers (API S3)                               | à venir |

## Architecture

### Le choix : monolithe modulaire

Un seul déployable (`gesta.jar`), plusieurs modules aux frontières vérifiées au build. Ce choix est délibéré : il donne les frontières de code d'une architecture distribuée sans ses coûts (réseau, pipelines multiples, observabilité distribuée), injustifiés à cette échelle — et chaque module reste extractible en service autonome si le besoin apparaît un jour.

```
              ┌──────────────────────────────┐
              │   Front React — SPA unique   │
              └───────────────┬──────────────┘
                              │  HTTPS · une seule URL
┌─────────────────────────────▼─────────────────────────────┐
│         gesta.jar — un seul processus Spring Boot         │
│                                                           │
│   gesta — portail : auth, accueil, assemblage             │
│      │                                                    │
│   resa · materio · odito · staff        métier            │
│      │                                                    │
│   ref · stocko                          transverse        │
│      │                                                    │
│   squelette                             socle commun      │
└──────────────┬─────────────────────────────┬──────────────┘
               ▼                             ▼
     ┌──────────────────┐          ┌──────────────────┐
     │    PostgreSQL    │          │    MinIO / S3    │
     │ 1 schéma/module  │          │    via stocko    │
     └──────────────────┘          └──────────────────┘
```

### Règles de dépendance

Les dépendances pointent toujours vers le bas, jamais latéralement ni vers le haut :

1. Tout module peut dépendre de `squelette` ; `squelette` ne dépend de personne.
2. Les modules métier (`resa`, `materio`, `odito`, `staff`) **ne se connaissent pas entre eux**. Toute collaboration passe par une interface publique ou un événement Spring, jamais par un import direct.
3. `ref` et `stocko` sont transverses : les modules métier consomment leur API publique (ex. `odito` enregistre ses documents générés via `stocko`).
4. `gesta` est le seul à tout connaître : il porte `@SpringBootApplication`, la configuration de sécurité, et assemble le jar final.

Ces règles sont **vérifiées à chaque build** : ArchUnit côté back, ESLint (`eslint-plugin-boundaries`) côté front.

### Arborescence

```
gesta/
├── pom.xml                # parent : versions, plugins, dépendances communes
├── squelette/             # socle : exceptions, audit, génériques, constantes
├── gesta/                 # application : auth, accueil, sécurité, main
├── docker-compose.yml     # PostgreSQL + MinIO (dev et démo)
│
│   # modules planifiés, non encore créés :
├── ref/                   # (à venir) référentiels (items, liens)
├── stocko/                # (à venir) abstraction stockage S3
├── resa/                  #  réservation de salles
├── materio/               # (à venir) gestion du matériel
├── odito/                 # (à venir) modèles de documents
├── staff/                 # (à venir) consultation SQL cataloguée
└── front/                 #  React (Vite), build embarqué dans le jar
    └── src/
        ├── app/           # router, providers, layout global
        ├── shared/        # ui/, api/, auth/, lib/ — le « squelette front »
        └── features/      # resa/, materio/, … un dossier par module
```

Côté front, mêmes règles qu'au back : `features → shared` autorisé, `shared → features` interdit, `features ↔ features` interdit (la navigation croisée passe par le router dans `app/`).

### Données

- Un seul PostgreSQL, **un schéma par module** (`gesta`, `resa`, …) : la frontière modulaire est visible jusqu'en base.
- Migrations **Liquibase** : un changelog par module, agrégés par un changelog maître dans `gesta`.
- Fichiers : API S3 consommée via `stocko`. MinIO en local et en démo ; le même code fonctionne sur un vrai bucket S3 sans modification.

### Sécurité

- Spring Security, authentification par **session** (cookie `HttpOnly`, protection CSRF) — le choix robuste pour une SPA servie par le backend (même origine, pas de CORS).
- Abstractions alignées OIDC : brancher un IdP externe (Keycloak) plus tard ne demanderait pas de refactor.
- Module `staff` : utilisateur BDD en **lecture seule** et catalogue de requêtes paramétrées — jamais de SQL libre saisi côté client.

## Stack

Java 25 (LTS) · Spring Boot 4 · Spring Security · JPA/Hibernate · Liquibase · PostgreSQL 18 · MinIO (API S3) · Maven multi-module · React + Vite · ArchUnit · Docker Compose — *prévu : Spring AI 2 + pgvector (chatbot)*
## Démarrage local

Prérequis : Java 25, Docker, Git.

```bash
# 1. Infrastructure (PostgreSQL + MinIO)
docker compose up -d

# 2. Lancement de l'application (depuis l'IDE, ou via Maven)
mvn spring-boot:run -pl gesta


```

- Application : http://localhost:8080
- Console MinIO : http://localhost:9001 (`minio` / `minio123`)

## Décisions d'architecture

| # | Décision                                  | Alternative écartée      | Pourquoi                                                                 |
|---|-------------------------------------------|--------------------------|--------------------------------------------------------------------------|
| 1 | Monolithe modulaire                       | Microservices            | Coûts distribués injustifiés à cette échelle ; frontières garanties par ArchUnit |
| 2 | Auth intégrée (sessions Spring Security)  | Keycloak                 | Un composant d'infra en moins ; couture OIDC prévue pour migrer plus tard |
| 3 | Maven multi-module                        | Lib partagée versionnée  | Refactors atomiques, zéro gestion de versions interne                    |
| 4 | SPA unique embarquée dans le jar          | Micro-frontends          | Une seule URL, pas de CORS, un seul déploiement                          |

## Roadmap

- **v1** : `gesta` (auth + accueil) + `resa` (réservation de salles), données de démonstration incluses, mise en ligne
- **Ensuite** : `materio`, puis `odito` + `stocko`, puis `ref` et `staff`
- **Enfin** : chatbot assistant — Spring AI 2 + pgvector sur le PostgreSQL existant
