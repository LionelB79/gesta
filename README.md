# Gesta

[![CI](https://github.com/LionelB79/gesta/actions/workflows/ci.yml/badge.svg)](https://github.com/LionelB79/gesta/actions/workflows/ci.yml)

Plateforme modulaire de gestion — réservation de salles, matériel, documents, référentiels — construite comme un **monolithe modulaire** Spring Boot + React.

> **Démo en ligne** : https://gesta-yh66.onrender.com — bouton « Connexion démo » sur la page de connexion (aucune inscription nécessaire).
> Hébergement gratuit : si l'application était en veille, le premier chargement peut prendre une à deux minutes.

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

Java 25 · Spring Boot 4 · Spring Security · JPA/Hibernate · Liquibase · PostgreSQL 18 · Maven multi-module · React 19 + TypeScript + Vite · React Router · JUnit · Mockito · AssertJ · Docker (image multi-étapes) · Docker Compose · oxlint · Render · Neon
*Prévu : MinIO (API S3), ArchUnit, Spring AI + pgvector (assistant).*

## Démarrage local

Prérequis : Java 25, Docker, Git. Node.js 24 uniquement pour le mode développement : le build Maven télécharge sa propre version de Node.

### Mode application (un seul jar, comme en production)

```bash
docker compose up -d
./mvnw clean package -pl gesta -am -DskipTests
java -jar gesta/target/gesta.jar
```

- Application : http://localhost:8080 — le front React est embarqué dans le jar.

### Mode conteneur (l'image Docker de production, avec le profil `prod`)

```bash
docker compose --profile app up --build
```

- Application : http://localhost:8080 — l'image est construite depuis le `Dockerfile`, et se connecte au PostgreSQL du Compose par le nom de service `postgres`.

### Mode développement (rechargement à chaud du front)

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

### Accès

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

## Configuration

Aucun secret n'est dans le dépôt. En développement, `application.yml` fournit des valeurs par défaut ; en production (profil `prod`), tout vient de variables d'environnement, sans valeur par défaut : l'application refuse de démarrer s'il en manque une.

| Variable                 | Rôle                                         | Défaut en développement                    |
|--------------------------|----------------------------------------------|--------------------------------------------|
| `SPRING_PROFILES_ACTIVE` | active le profil `prod`                      | aucun profil                               |
| `DB_URL`                 | URL JDBC de PostgreSQL                       | `jdbc:postgresql://localhost:5433/gesta`   |
| `DB_USERNAME`            | utilisateur PostgreSQL                       | `gesta`                                    |
| `DB_PASSWORD`            | mot de passe PostgreSQL                      | `gesta`                                    |
| `PORT`                   | port HTTP (imposé par l'hébergeur)           | `8080`                                     |

Le profil `prod` ajoute : cookie de session `Secure`, prise en compte des en-têtes `X-Forwarded-*` du proxy de l'hébergeur, pool de connexions qui se vide quand l'application est inactive, détails de santé masqués, sonde de vivacité `/actuator/health/liveness`.

## Déploiement

| Élément | Service | Rôle |
|---|---|---|
| Application | **Render** (offre gratuite, région Francfort) | construit l'image depuis le `Dockerfile` à chaque push sur `main`, fournit l'URL et le HTTPS |
| Base de données | **Neon** (PostgreSQL 18 managé, offre gratuite, Francfort) | migrée par Liquibase au démarrage ; se met en veille après 5 minutes d'inactivité |
| Disponibilité | **UptimeRobot** | appelle la page d'accueil toutes les 5 minutes, pour éviter la mise en veille de Render, et alerte en cas de panne |

Points d'attention :

- **Contrôle de santé Render** sur `/actuator/health/liveness`, qui n'interroge pas la base : sinon Neon ne se mettrait jamais en veille et le quota gratuit serait épuisé.
- **Mémoire** : 512 Mo, d'où `-XX:MaxRAMPercentage=75` dans le `Dockerfile`.
- **Démarrage** : environ deux minutes sur 0,1 CPU.

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
| 11 | Front construit par Maven (`frontend-maven-plugin`, Node téléchargé par le build) | Build du front séparé, à la main ou en CI | Un seul `./mvnw package` produit le livrable complet, avec une version de Node identique sur toutes les machines |
| 12 | Sécurité par préfixe : `/api/**` authentifié, le reste (front) public, repli SPA vers `index.html` hors `/api` | Lister chaque route du front côté serveur | Le serveur n'a pas à connaître les routes React ; contrepartie : toute route d'API doit commencer par `/api` |
| 13 | Configuration par variables d'environnement, profil `prod` sans valeur par défaut | Secrets dans `application-prod.yml` | Un secret commité reste dans l'historique Git et finit dans le jar et l'image ; sans variable, l'application refuse de démarrer |
| 14 | Image Docker multi-étapes, exécutée par un utilisateur non `root` | Image unique avec JDK et outils de build | Image finale plus légère, sans code source ni outils ; surface d'attaque réduite |
| 15 | Hébergement Render (application) + Neon (PostgreSQL), à 0 € | VM Oracle Cloud gratuite ; base PostgreSQL gratuite de Render | Mise en ligne rapide sans administrer de serveur ; la base de Render expire après 30 jours, celle de Neon n'a pas de limite de durée |

## Roadmap

- **v1 (en cours)** : `gesta` + `resa` ✅, jar unique avec le front embarqué ✅, image Docker et mise en ligne ✅ — reste : réinitialisation automatique des données de démo, intégration continue (GitHub Actions), documentation OpenAPI, tests de slice (`@WebMvcTest`, `@DataJpaTest`), règles ArchUnit
- **Ensuite** : `materio`, puis `odito` + `stocko`, puis `ref` et `staff`
- **Enfin** : assistant — Spring AI + pgvector sur le PostgreSQL existant