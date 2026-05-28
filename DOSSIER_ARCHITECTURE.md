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

- `login` : authentification via matricule ;
- `home` : page d'accueil après connexion ;
- `navbar` : navigation principale ;
- `match-list` : liste des matchs disponibles ;
- `create-match` : création d'un match ;
- `reservation` et `mes-reservations` : gestion et affichage des réservations ;
- `mon-compte` : informations du membre connecté ;
- `admin` : tableau de bord administratif.

L'application utilise des composants standalone Angular et plusieurs modules Angular Material pour les cartes, boutons, formulaires, champs de saisie, icônes et sidenav.

### Services

Les services sont placés dans `frontend/src/app/core/services` :

- `AuthService` : connexion, inscription, stockage de l'utilisateur dans le `localStorage`, rôle et type de membre ;
- `MatchService` : accès aux matchs, terrains, sites et créneaux disponibles ;
- `ReservationService` : rejoindre un match, ajouter un joueur à un match privé, consulter et payer des réservations ;
- `AdminService` : récupération des indicateurs d'administration.

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

### Modèles frontend

Les modèles TypeScript sont dans `frontend/src/app/core/models` :

- `membre.ts` ;
- `match.ts` ;
- `reservation.ts` ;
- `site.ts` ;
- `terrain.ts`.

Ils représentent les données échangées avec le backend et utilisées dans les composants.

### Communication HTTP

Le frontend communique avec le backend via des URL commençant par `/api`.

Le fichier `frontend/proxy.conf.json` redirige `/api` vers `http://localhost:8080` et retire le préfixe `/api`. Par exemple, `/api/auth/login` côté Angular correspond à `/auth/login` côté Spring Boot.

## Architecture backend Spring Boot

Le backend est une application Spring Boot située dans `backend/padel`. La classe principale est `PadelApplication`.

### Controllers

Les contrôleurs REST se trouvent dans `backend/padel/src/main/java/be/ephec/pdw/padel/controllers` :

- `AuthController` : authentification et gestion des utilisateurs ;
- `MatchController` : création de matchs, matchs disponibles, joueurs inscrits, terrains, créneaux et sites ;
- `ReservationController` : rejoindre un match, ajouter un joueur à un match privé, payer une réservation, consulter les réservations d'un membre ;
- `AdminController` : indicateurs administratifs.

### Services

Les services métier se trouvent dans le package `service` :

- `AuthService` ;
- `MatchService` ;
- `ReservationService` ;
- `TerrainService` ;
- `AdminService`.

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

### Models et entities

Les entités JPA sont dans le package `model` :

- `Membre`, avec héritage `JOINED` ;
- `MembreGlobal`, `MembreSite`, `MembreLibre` ;
- `Site` ;
- `Terrain` ;
- `Match` ;
- `Reservation` ;
- `JourFermeture`.

Les énumérations présentes sont `Role`, `StatutMatch` et `StatutReservation`.

### DTO

Les DTO sont regroupés dans le package `dto` :

- `LoginDTO` ;
- `RegisterDTO` ;
- `MatchDTO` et `MatchReponseDTO` ;
- `ReservationDTO` et `ReservationReponseDTO` ;
- `TerrainDTO` ;
- `JoueurDTO`.

### Exceptions

Le package `configuration` contient :

- `BusinessRuleException` ;
- `ForbiddenException` ;
- `GlobalExceptionHandler` ;
- `CorsConfig`.

`GlobalExceptionHandler` transforme les exceptions métier en réponses HTTP adaptées, notamment `400 Bad Request` et `403 Forbidden`.

## Base de données

La base configurée est MySQL, via `application.properties` :

- URL : `jdbc:mysql://localhost:3307/padel_db` ;
- utilisateur : `sa` ;
- mot de passe : `password`.

Hibernate est configuré avec `spring.jpa.hibernate.ddl-auto=update`, ce qui permet de mettre à jour le schéma à partir des entités JPA au démarrage.

Relations principales :

- un `Site` possède plusieurs `Terrain` ;
- un `Site` possède plusieurs `JourFermeture` ;
- un `Terrain` appartient à un `Site` ;
- un `Match` est lié à un `Terrain` et à un membre organisateur ;
- un `Match` possède plusieurs `Reservation` ;
- une `Reservation` est liée à un `Membre` et à un `Match` ;
- `MembreSite` est lié à un `Site`.

## Librairies, outils et frameworks structurants

- Angular 21 ;
- Angular Material ;
- RxJS ;
- TypeScript ;
- Tailwind CSS (partiellement utilisé) ;
- Spring Boot ;
- Maven ;
- Spring Web MVC ;
- Spring Data JPA ;
- Hibernate ;
- MySQL Connector/J ;
- Lombok ;
- Springdoc OpenAPI / Swagger UI ;
- Vitest pour les tests frontend ;
- JUnit / Spring Boot Test pour les tests backend.

Le dépôt ne contient pas de fichier Docker ou `docker-compose`. Pour l'environnement local, MySQL peut être lancé avec une commande Docker exposant le port `3307`.
