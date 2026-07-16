# Dossier d'architecture

## Présentation du projet

Ce projet est une application de gestion de padel. Il permet de gérer des membres, des sites, des terrains, des matchs, des réservations et une partie administration.

L'application est organisée en deux parties :

- un frontend Angular dans le dossier `frontend` ;
- un backend Spring Boot dans le dossier `backend/padel`.

## Type d'architecture

Selon les modèles présentés dans le cours, le projet se rapproche principalement d'une architecture en couches classiques.

Plus précisément :

- le frontend Angular se rapproche d'une organisation MVC / MVVM : les composants gèrent l'affichage et les interactions utilisateur, les services réalisent les appels HTTP et les modèles structurent les données ;
- le backend Spring Boot suit une architecture 3-tier : les contrôleurs représentent la couche présentation/API, les services contiennent la logique métier et les repositories assurent l'accès aux données ;
- la base de données MySQL constitue la couche de persistance.

## Architecture frontend Angular

Le frontend est une application Angular générée avec Angular CLI. Le point d'entrée est `src/main.ts`, qui démarre le composant principal `App` avec la configuration `app.config.ts`.

### Composants

Les composants se trouvent principalement dans `frontend/src/app/core/components` :

- `login` : connexion et inscription ;
- `home` : page d'accueil ;
- `navbar` : navigation principale ;
- `match-list` : liste des matchs disponibles ;
- `create-match` : création d'un match ;
- `reservation` et `mes-reservations` : gestion et affichage des réservations ;
- `mon-compte` : informations du membre connecté ;
- `admin` : tableau de bord administratif.

L'application utilise des composants standalone Angular et plusieurs modules Angular Material pour les cartes, boutons, formulaires, champs de saisie, icônes et sidenav.

### Services

Les services sont placés dans `frontend/src/app/core/services` :

- `AuthService` : connexion, inscription et session ;
- `MatchService` : matchs, terrains, sites et créneaux ;
- `ReservationService` : participation, paiement et réservations ;
- `AdminService` : données du dashboard ;
- `NotificationService` : notifications snackbar.

Ces services utilisent `HttpClient`, fourni dans `app.config.ts`.

### Routing

Les routes sont définies dans `frontend/src/app/app.routes.ts`.

Routes principales :

- `/login` ;
- `/home` ;
- `/match` ;
- `/reservation` ;
- `/mes-reservations` ;
- `/mon-compte` ;
- `/create-match` ;
- `/admin`.

Les routes fonctionnelles sont protégées par `authGuard`. La route `/admin` utilise aussi `adminGuard`.
Le JWT et l'utilisateur sont stockés dans `sessionStorage`. `authInterceptor` ajoute le JWT aux requêtes avec l'en-tête `Authorization: Bearer ...`.

### Communication HTTP

Le frontend communique avec le backend via des URL commençant par `/api`.

Le fichier `frontend/proxy.conf.json` redirige `/api` vers `http://localhost:8080` et retire le préfixe `/api`. Par exemple, `/api/auth/login` côté Angular correspond à `/auth/login` côté Spring Boot.

## Architecture backend Spring Boot

Le backend est une application Spring Boot située dans `backend/padel`. La classe principale est `PadelApplication`.

### Controllers

Les controllers REST sont dans le package `controllers` :

- `AuthController` : connexion, inscription et mot de passe ;
- `MatchController` : matchs, terrains, sites et créneaux ;
- `ReservationController` : participation, paiement et réservations ;
- `AdminController` : indicateurs et dashboard administratif.

Les entrées sont validées avec `@Valid`. Les controllers délèguent ensuite le traitement aux services.

### Services

Les services métier se trouvent dans le package `service` :

- `AuthService` ;
- `MatchService` ;
- `ReservationService` ;
- `TerrainService` ;
- `AdminService`;
- `CurrentUserService` ;
- `LoginAttemptService`.

Ils centralisent les règles de gestion : délais de réservation, validation des membres, disponibilité des terrains, statut des matchs, paiement et indicateurs d'administration.

### Repositories

Les repositories Spring Data JPA se trouvent dans le package `repositories` :

- `MembreRepository` ;
- `MatchRepository` ;
- `ReservationRepository` ;
- `TerrainRepository` ;
- `SiteRepository` ;
- `JourFermetureRepository`.

Ils étendent `JpaRepository` et assurent l'accès aux données.

Des requêtes JPQL et des projections sont utilisées pour les statistiques du dashboard.
### Modèles et DTO

Les entités JPA se trouvent dans le package `model` :

- `Membre`, avec héritage `JOINED` ;
- `MembreGlobal`, `MembreSite` et `MembreLibre` ;
- `Site`, `Terrain`, `Match`, `Reservation` et `JourFermeture`.

Les énumérations principales sont `Role`, `StatutMatch` et `StatutReservation`.

Le package `dto` contient les entrées et sorties de l'API : connexion, inscription, session, matchs, réservations, terrains, joueurs et dashboard administratif.

### Packages techniques

- `configuration` : CORS, Spring Security, Jackson et BCrypt ;
- `security` : JWT et filtre d'authentification ;
- `exception` : exceptions, `GlobalExceptionHandler` et `ErrorResponse` ;
- `scheduler` : tâches automatiques ;
- `seed` : données de démonstration du profil `dev`.

## Base de données

La base configurée est MySQL, via `application.properties` :

- URL : `jdbc:mysql://localhost:3307/padel_db` ;
- utilisateur : `sa` ;
- mot de passe : `password`.

Hibernate est configuré avec `spring.jpa.hibernate.ddl-auto=update`, ce qui permet de mettre à jour le schéma à partir des entités JPA au démarrage.

Relations principales :

- un `Site` possède plusieurs `Terrain` et plusieurs `JourFermeture` ;
- un `Terrain` appartient à un `Site` ;
- un `Match` est lié à un `Terrain` et à un membre organisateur ;
- un `Match` possède plusieurs `Reservation` ;
- une `Reservation` est liée à un `Membre` et à un `Match` ;
- un `MembreSite` est lié à un `Site`.

Les tests d'intégration utilisent une base H2 en mémoire.

## Sécurité

L'authentification se fait par matricule. Les utilisateurs ordinaires n'ont pas de mot de passe. Un mot de passe est demandé uniquement aux administrateurs et il est encodé avec BCrypt.

Après la connexion, le backend génère un JWT contenant le matricule et le rôle. Spring Security et `JwtAuthenticationFilter` valident ce jeton pour les endpoints protégés.

Les rôles sont `USER`, `ADMIN_SITE` et `ADMIN_GLOBAL`. Un administrateur de site est limité aux données de son site. Un administrateur global peut accéder aux différents sites. Les guards Angular protègent la navigation, mais les autorisations réelles sont vérifiées par le backend.

## Gestion des erreurs

Les DTO utilisent Jakarta Validation pour vérifier les données reçues.

`GlobalExceptionHandler` transforme les exceptions en réponses HTTP communes. `ErrorResponse` contient le statut, le message, la date et les éventuelles erreurs de champs.

Angular lit ce format avec `api-error.util.ts`. Les erreurs sont affichées dans les formulaires ou avec les snackbars de `NotificationService`.

## Tests

Le backend contient des tests unitaires, des tests de controllers, des tests d'intégration avec H2 et des tests de sécurité. Ils couvrent notamment les règles métier, les transactions, JWT, les autorisations intersites, les statuts et le scheduler.

Le frontend teste les composants, services, guards, notifications et erreurs avec Vitest. Cypress est configuré pour trois parcours E2E : connexion, consultation des matchs et consultation des réservations.

Résultats vérifiés le 16 juillet 2026 :

- **141 tests backend réussis** avec `mvn test` ;
- **51 tests frontend réussis** avec `npm test -- --watch=false`.

## Librairies, outils et frameworks structurants

- Angular 21.2, Angular Material 21.2 et Tailwind CSS 4.2 ;
- Java 25 et Spring Boot 4.0.3 ;
- Spring Web MVC et Spring Security ;
- BCrypt et JJWT 0.12.6 ;
- Spring Data JPA, Hibernate et MySQL ;
- Maven ;
- Cypress 15.18.1 ;
- Springdoc OpenAPI 2.8.13 et Swagger UI.

Le dépôt ne contient pas de fichier Docker ou `docker-compose`. Pour l'environnement local, MySQL peut être lancé avec une commande Docker exposant le port `3307`.
