# Gesta

Plateforme modulaire de gestion — réservation de salles, matériel, documents, référentiels — construite comme un **monolithe modulaire** Spring Boot + React.

> **Démo en ligne** : _URL à venir_ — bouton « Connexion démo » sur la page de connexion (aucune inscription nécessaire).

## Vue d'ensemble

Gesta est le portail (authentification + accueil) qui héberge des modules métier aux frontières strictes :

| Module      | Rôle                                                                  | Statut  |
|-------------|-----------------------------------------------------------------------|---------|
| `gesta`     | Portail : auth, sécurité, assemblage de l'application                 | v1      |
| `resa`      | Réservation de salles                                                 | v1      |
| `squelette` | Socle commun : hiérarchie d'exceptions métier, gestion d'erreurs REST | v1      |
| `materio`   | Gestion du matériel                                                   | à venir |
| `odito`     | Édition de modèles de documents (variables stat./dyn.)                | à venir |
| `staff`     | Consultation de données via requêtes SQL cataloguées                  | à venir |
| `ref`       | Référentiels partagés (items, liens)                                  | à venir |
| `stocko`    | Stockage de fichiers (API S3)                                         | à venir |

## Fonctionnalités (v1)

- **Connexion par session**, avec un compte de démonstration accessible en un clic.
- **Planning des salles** : liste paginée et triée, filtrable par salle, état et date de début.
- **Réservation** d'une salle active, avec interdiction des chevauchements **garantie en base**, y compris sous accès concurrents.
- **Annulation logique** de ses propres réservations (la réservation reste dans l'historique).
- **Statistiques** : nombre de réservations confirmées par salle, salles vides comprises.
- **Erreurs uniformes** : toute erreur (métier, validation, sécurité, technique) est renvoyée au même format JSON.

## Architecture

### Le choix : monolithe modulaire

Un seul déployable (`gesta.jar`), plusieurs modules Maven aux frontières explicites. Ce choix est délibéré : il donne les frontières de code d'une architecture distribuée sans ses coûts (réseau, pipelines multiples, observabilité distribuée), injustifiés à cette échelle — et chaque module reste extractible en service autonome si le besoin apparaît un jour.

```
              ┌──────────────────────────────┐
              │   Front React — SPA unique   │
              └───────────────┬──────────────┘
                              │  HTTPS · une seule URL
┌─────────────────────────────▼─────────────────────────────┐
│         gesta.jar — un seul processus Spring Boot         │
│                                                           │
│   gesta — portail : auth, sécurité, assemblage            │
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
     │ 1 schéma/module  │          │ (prévu, stocko)  │
     └──────────────────┘          └──────────────────┘
```

### Règles de dépendance

Les dépendances pointent toujours vers le bas, jamais latéralement ni vers le haut :

1. Tout module peut dépendre de `squelette` ; `squelette` ne dépend de personne.
2. Les modules métier (`resa`, `materio`, `odito`, `staff`) **ne se connaissent pas entre eux**. Toute collaboration passe par une interface publique ou un événement Spring, jamais par un import direct.
3. `ref` et `stocko` sont transverses : les modules métier consomment leur API publique.
4. `gesta` est le seul à tout connaître : il porte `@SpringBootApplication`, la configuration de sécurité, et assemble le jar final.

**Aujourd'hui**, ces règles sont garanties par les dépendances Maven : un module ne peut importer que ce que son `pom.xml` déclare.
**Prévu** : des règles ArchUnit côté back et des règles d'import oxlint côté front, pour vérifier aussi les dépendances entre packages à chaque build.

### Arborescence

```
gesta/
├── pom.xml                  # parent : versions, modules, dépendances communes
├── mvnw, mvnw.cmd, .mvn/    # Maven Wrapper (aucune installation de Maven requise)
├── docker-compose.yml       # PostgreSQL, MinIO, pgAdmin
├── docker/postgres-init/    # création de la base de test au premier démarrage
├── squelette/               # socle : exceptions métier, gestion d'erreurs REST
├── resa/                    # réservation de salles (entités, services, API, migrations)
├── gesta/                   # application : main, sécurité, auth, changelog maître
└── front/                   # React + TypeScript (Vite)
    └── src/
        ├── app/             # router
        ├── shared/          # api/ (client HTTP, erreurs), auth/ (contexte, route protégée)
        └── features/        # auth/, bookings/ — un dossier par fonctionnalité
```

Côté front, mêmes règles qu'au back : `features → shared` autorisé, `shared → features` interdit, `features ↔ features` interdit (la navigation croisée passe par le router dans `app/`).

### Données

- Un seul PostgreSQL, **un schéma par module** (`gesta`, `resa`, …) : la frontière modulaire est visible jusqu'en base.
- Migrations **Liquibase** : un changelog par module, agrégés par un changelog maître dans `gesta`.
- **Pas de double réservation, même en concurrence** : une contrainte d'exclusion PostgreSQL (`btree_gist`, `tstzrange`) interdit deux réservations confirmées qui se chevauchent sur une même salle. Le service vérifie aussi en amont pour renvoyer un message clair, et rejoue la transaction en cas d'interblocage.

### Gestion des erreurs

Les services lèvent des **exceptions métier sans notion HTTP**. Le module `squelette` les classe par catégorie, et un seul composant traduit chaque catégorie en statut :

| Catégorie            | Statut | Exemple                          |
|----------------------|--------|----------------------------------|
| `NotFoundException`  | 404    | salle ou réservation introuvable |
| `ConflictException`  | 409    | créneau déjà pris                |
| `ForbiddenException` | 403    | annuler la réservation d'un autre |
| `BusinessRuleException` | 400 | salle désactivée, période invalide |

Les erreurs de Spring MVC (JSON illisible, méthode non supportée, validation…) et celles de Spring Security (401, 403 CSRF) sont ramenées au même format :

```json
{ "timestamp": "…", "status": 409, "code": "OVERLAP_ERROR", "message": "…", "path": "/api/bookings" }
```

### Sécurité

- Spring Security, authentification par **session** (cookie `HttpOnly`) — le choix robuste pour une SPA servie par le backend (même origine, pas de CORS).
- **CSRF** : cookie `XSRF-TOKEN` lu par le front et renvoyé en en-tête `X-XSRF-TOKEN` sur toute requête qui modifie des données.
- **Changement d'identifiant de session** à la connexion (protection contre la fixation de session).
- Mots de passe hachés en **BCrypt** ; un identifiant inconnu et un mot de passe faux renvoient la même erreur (pas d'énumération de comptes).
- Évolution envisagée : un fournisseur d'identité externe (OIDC, Keycloak).

## API (v1)

| Méthode | Route                         | Rôle                                                        |
|---------|-------------------------------|-------------------------------------------------------------|
| POST    | `/api/login`                  | connexion (crée la session)                                 |
| POST    | `/api/logout`                 | déconnexion                                                 |
| GET     | `/api/me`                     | utilisateur connecté                                        |
| GET     | `/api/rooms`                  | salles actives, triées par nom                              |
| GET     | `/api/bookings`               | réservations paginées ; filtres `roomId`, `state`, `start` ; `page`, `size`, `sort` |
| POST    | `/api/bookings`               | créer une réservation                                       |
| POST    | `/api/bookings/{id}/cancel`   | annuler une de ses réservations                             |
| GET     | `/api/bookings/stats`         | réservations confirmées par salle                           |

Documentation OpenAPI : prévue.

## Stack

Java 25 · Spring Boot 4 · Spring Security · JPA/Hibernate · Liquibase · PostgreSQL 18 · Maven multi-module · React 19 + TypeScript + Vite · React Router · JUnit · Mockito · AssertJ · Docker Compose · oxlint
*Prévu : MinIO (API S3), ArchUnit, Spring AI + pgvector (assistant).*

## Démarrage local

Prérequis : Java 25, Docker, Node.js 22+, Git.

```bash
# 1. Infrastructure (PostgreSQL, MinIO, pgAdmin)
docker compose up -d

# 2. Back-end (depuis la racine)
./mvnw spring-boot:run -pl gesta -am

# 3. Front-end (dans un second terminal)
cd front
npm install
npm run dev
```

- Application : http://localhost:5173 (le serveur Vite redirige `/api` vers le back sur le port 8080)
- Compte de démonstration : `demo` / `demo2026!`
- pgAdmin : http://localhost:5050 (serveur PostgreSQL : `localhost:5433`, `gesta` / `gesta`)

## Tests

```bash
./mvnw test
```

Docker doit être démarré : les tests d'intégration de `resa` tournent sur une base dédiée `gesta_test`, migrée par Liquibase au lancement, et ne touchent jamais la base de développement. Ils incluent un test de réservations concurrentes sur le même créneau.

La base `gesta_test` est créée automatiquement au premier démarrage du conteneur. Sur un volume Docker déjà existant :

```bash
docker exec gesta-postgres psql -U gesta -d gesta -c "CREATE DATABASE gesta_test"
```

## Décisions d'architecture

| #  | Décision | Alternative écartée | Pourquoi |
|----|----------|---------------------|----------|
| 1  | Monolithe modulaire | Microservices | Coûts distribués injustifiés à cette échelle ; frontières garanties par les modules Maven |
| 2  | Auth intégrée (sessions Spring Security) | Keycloak, JWT | Un composant d'infra en moins ; session révocable, cookie `HttpOnly` |
| 3  | Maven multi-module | Lib partagée versionnée | Refactors atomiques, zéro gestion de versions interne |
| 4  | SPA unique embarquée dans le jar | Micro-frontends | Une seule URL, pas de CORS, un seul déploiement |
| 5  | Liquibase | Flyway | Rollback natif, changelogs composables par module |
| 6  | Exceptions métier sans HTTP, classées par catégorie | Exceptions HTTP levées par les services | Métier indépendant du transport ; statut décidé à un seul endroit |
| 7  | Chevauchement : contrainte d'exclusion en base + vérification applicative + réessai sur interblocage | Vérification applicative seule | Seule la base garantit l'invariant sous concurrence ; la vérification donne un message clair ; le réessai transforme un interblocage en 409 au lieu d'un 500 |
| 8  | Annulation logique, action `POST /bookings/{id}/cancel` | `DELETE` physique, `PATCH` de l'état | Historique, audit et statistiques conservés ; une transition gouvernée par des règles métier s'expose comme une action explicite |
| 9  | Planning partagé, actions réservées au propriétaire | Ne montrer que ses propres réservations | Pour choisir un créneau, il faut voir l'occupation des salles |
| 10 | Base de test dédiée, migrée par Liquibase | Tests sur la base de dev | Les tests ne détruisent pas les données de développement et ne dépendent pas d'un lancement préalable de l'application |

## Roadmap

- **v1 (en cours)** : `gesta` + `resa` ✅ — reste : mise en ligne (jar unique avec le front embarqué), intégration continue (GitHub Actions), documentation OpenAPI, tests de slice (`@WebMvcTest`, `@DataJpaTest`), règles ArchUnit
- **Ensuite** : `materio`, puis `odito` + `stocko`, puis `ref` et `staff`
- **Enfin** : assistant — Spring AI + pgvector sur le PostgreSQL existant