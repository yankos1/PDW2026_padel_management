package be.ephec.pdw.padel.seed;

import be.ephec.pdw.padel.model.JourFermeture;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.StatutReservation;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {
    private final SiteRepository siteRepository;
    private final TerrainRepository terrainRepository;
    private final MembreRepository membreRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-default-password:}")
    private String adminDefaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedSitesTerrainsEtFermetures();
        seedMembres();
        seedMatchsEtReservations();
    }

    private void seedSitesTerrainsEtFermetures() {
        Site bruxelles = getOrCreateSite("Padel Bruxelles", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Site wavre = getOrCreateSite("Padel Wavre", LocalTime.of(9, 0), LocalTime.of(21, 0));

        getOrCreateTerrain("Bruxelles 1", bruxelles);
        getOrCreateTerrain("Bruxelles 2", bruxelles);
        getOrCreateTerrain("Wavre 1", wavre);
        getOrCreateTerrain("Wavre 2", wavre);

        getOrCreateJourFermeture(bruxelles, LocalDate.now().plusDays(10));
        getOrCreateJourFermeture(wavre, LocalDate.now().plusDays(14));

        log.info("Donnees de developpement creees: sites, terrains et jours de fermeture");
    }

    private void seedMembres() {
        Site site = findSiteByName("Padel Bruxelles");

        initialiserMotDePasseAdmin(
                getOrCreateMembreGlobal("G0001", "Admin", "Global", "admin.global@example.test", Role.ADMIN_GLOBAL)
        );
        initialiserMotDePasseAdmin(
                getOrCreateMembreGlobal("G0101", "Administrateur", "Test", "admin.test@padel.local", Role.ADMIN_GLOBAL)
        );
        initialiserMotDePasseAdmin(
                getOrCreateMembreGlobal("G0102", "Administrateur Deux", "Test", "admin.test2@padel.local", Role.ADMIN_GLOBAL)
        );
        getOrCreateMembreGlobal("G0002", "Alice", "Martin", "alice.martin@example.test", Role.USER);
        getOrCreateMembreGlobal("G0003", "Emma", "Bernard", "emma.bernard@example.test", Role.USER);
        getOrCreateMembreGlobal("G0004", "Lucas", "Petit", "lucas.petit@example.test", Role.USER);
        getOrCreateMembreGlobal("G0005", "Mila", "Robert", "mila.robert@example.test", Role.USER);
        initialiserMotDePasseAdmin(
                getOrCreateMembreSite("S0001", "Admin", "Site", "admin.site@example.test", Role.ADMIN_SITE, site)
        );
        getOrCreateMembreSite("S0002", "Bruno", "Lambert", "bruno.lambert@example.test", Role.USER, site);
        getOrCreateMembreSite("S0003", "Nora", "Dubois", "nora.dubois@example.test", Role.USER, site);
        getOrCreateMembreSite("S0004", "Hugo", "Moreau", "hugo.moreau@example.test", Role.USER, site);
        getOrCreateMembreSite("S0005", "Lea", "Simon", "lea.simon@example.test", Role.USER, site);
        getOrCreateMembreLibre("L0001", "Chloe", "Durand", "chloe.durand@example.test");
        getOrCreateMembreLibre("L0002", "Noah", "Leroy", "noah.leroy@example.test");
        getOrCreateMembreLibre("L0003", "Ines", "Roux", "ines.roux@example.test");
        getOrCreateMembreLibre("L0004", "Louis", "Fournier", "louis.fournier@example.test");
        getOrCreateMembreLibre("L0005", "Zoe", "Girard", "zoe.girard@example.test");

        log.info("Donnees de developpement creees: membres et administrateurs");
    }

    private void seedMatchsEtReservations() {
        Terrain terrainPublic = findTerrainByNameAndSiteName("Bruxelles 1", "Padel Bruxelles");
        Terrain terrainPrive = findTerrainByNameAndSiteName("Bruxelles 2", "Padel Bruxelles");
        MembreGlobal membreGlobal = findMembreGlobal("G0002");
        MembreSite membreSite = findMembreSite("S0002");
        MembreLibre membreLibre = findMembreLibre("L0001");

        if (terrainPublic == null || terrainPrive == null || membreGlobal == null || membreSite == null || membreLibre == null) {
            log.warn("Seed dev ignore: donnees necessaires manquantes pour creer les matchs");
            return;
        }

        Match matchPublic = getOrCreateMatch(membreGlobal, terrainPublic, dateDans(3, 10, 0), true);
        Match matchPrive = getOrCreateMatch(membreSite, terrainPrive, dateDans(4, 14, 0), false);

        getOrCreateReservation(matchPublic, membreGlobal, true);
        getOrCreateReservation(matchPublic, membreLibre, false);
        getOrCreateReservation(matchPrive, membreSite, true);

        log.info("Donnees de developpement creees: matchs et reservations");
    }

    private Site getOrCreateSite(String name, LocalTime ouverture, LocalTime fermeture) {
        Site existing = findSiteByName(name);

        if (existing != null) {
            return existing;
        }

        return siteRepository.save(creerSite(name, ouverture, fermeture));
    }

    private Site findSiteByName(String name) {
        return siteRepository.findAll()
                .stream()
                .filter(site -> name.equals(site.getName()))
                .findFirst()
                .orElse(null);
    }

    private Terrain getOrCreateTerrain(String nom, Site site) {
        Terrain existing = findTerrainByNameAndSite(nom, site);

        if (existing != null) {
            return existing;
        }

        return terrainRepository.save(creerTerrain(nom, site));
    }

    private Terrain findTerrainByNameAndSiteName(String nom, String siteName) {
        Site site = findSiteByName(siteName);

        if (site == null) {
            return null;
        }

        return findTerrainByNameAndSite(nom, site);
    }

    private Terrain findTerrainByNameAndSite(String nom, Site site) {
        return terrainRepository.findBySiteId(site.getId())
                .stream()
                .filter(terrain -> nom.equals(terrain.getNom()))
                .findFirst()
                .orElse(null);
    }

    private void getOrCreateJourFermeture(Site site, LocalDate date) {
        if (!jourFermetureRepository.existsBySiteAndDate(site, date)) {
            jourFermetureRepository.save(JourFermeture.builder()
                    .site(site)
                    .date(date)
                    .build());
        }
    }

    private Membre getOrCreateMembreGlobal(String matricule, String prenom, String nom, String email, Role role) {
        return membreRepository.findById(matricule)
                .orElseGet(() -> membreRepository.save(creerMembreGlobal(matricule, prenom, nom, email, role)));
    }

    private Membre getOrCreateMembreSite(String matricule, String prenom, String nom, String email, Role role, Site site) {
        return membreRepository.findById(matricule)
                .orElseGet(() -> membreRepository.save(creerMembreSite(matricule, prenom, nom, email, role, site)));
    }

    void initialiserMotDePasseAdmin(Membre membre) {
        if (membre == null || (membre.getRole() != Role.ADMIN_GLOBAL && membre.getRole() != Role.ADMIN_SITE)) {
            return;
        }
        if (membre.getAdminPasswordHash() != null && !membre.getAdminPasswordHash().isBlank()) {
            return;
        }
        if (adminDefaultPassword == null || adminDefaultPassword.isBlank()) {
            log.warn("Mot de passe administrateur initial non configure: le compte {} ne pourra pas se connecter",
                    membre.getMatricule());
            return;
        }

        membre.setAdminPasswordHash(passwordEncoder.encode(adminDefaultPassword));
        membreRepository.save(membre);
    }

    private void getOrCreateMembreLibre(String matricule, String prenom, String nom, String email) {
        if (membreRepository.existsById(matricule)) {
            return;
        }

        membreRepository.save(creerMembreLibre(matricule, prenom, nom, email));
    }

    private Match getOrCreateMatch(Membre organisateur, Terrain terrain, LocalDateTime dateHeureDebut, boolean estPublic) {
        return matchRepository.findByTerrain(terrain)
                .stream()
                .filter(match -> match.getOrganisateur() != null)
                .filter(match -> organisateur.getMatricule().equals(match.getOrganisateur().getMatricule()))
                .filter(match -> match.isEstPublic() == estPublic)
                .findFirst()
                .orElseGet(() -> {
                    Membre organisateurPersistant = membreRepository.findById(organisateur.getMatricule())
                            .orElseThrow();

                    return matchRepository.save(creerMatch(organisateurPersistant, terrain, dateHeureDebut, estPublic));
                });
    }

    private MembreGlobal findMembreGlobal(String matricule) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        return membre instanceof MembreGlobal membreGlobal ? membreGlobal : null;
    }

    private MembreSite findMembreSite(String matricule) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        return membre instanceof MembreSite membreSite ? membreSite : null;
    }

    private MembreLibre findMembreLibre(String matricule) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        return membre instanceof MembreLibre membreLibre ? membreLibre : null;
    }

    private void getOrCreateReservation(Match match, Membre membre, boolean estPayee) {
        if (!reservationRepository.existsByMatchAndMembre(match, membre)) {
            reservationRepository.save(creerReservation(match, membre, estPayee));
        }
    }

    private Site creerSite(String name, LocalTime ouverture, LocalTime fermeture) {
        return Site.builder()
                .name(name)
                .heureOuverture(ouverture)
                .heureFermeture(fermeture)
                .build();
    }

    private Terrain creerTerrain(String nom, Site site) {
        return Terrain.builder()
                .nom(nom)
                .site(site)
                .build();
    }

    private Match creerMatch(Membre organisateur, Terrain terrain, LocalDateTime dateHeureDebut, boolean estPublic) {
        return Match.builder()
                .organisateur(organisateur)
                .terrain(terrain)
                .dateHeureDebut(dateHeureDebut)
                .estPublic(estPublic)
                .statut(StatutMatch.PLANIFIE)
                .build();
    }

    private LocalDateTime dateDans(int jours, int heure, int minute) {
        return LocalDateTime.now()
                .plusDays(jours)
                .withHour(heure)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);
    }

    private Reservation creerReservation(Match match, Membre membre, boolean estPayee) {
        return Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .datePaiement(estPayee ? LocalDateTime.now() : null)
                .montant(estPayee ? 15 : 0)
                .estPayee(estPayee)
                .statut(estPayee ? StatutReservation.CONFIRMEE : StatutReservation.EN_ATTENTE)
                .build();
    }

    private MembreGlobal creerMembreGlobal(String matricule, String prenom, String nom, String email, Role role) {
        MembreGlobal membre = new MembreGlobal();
        renseignerMembre(membre, matricule, prenom, nom, email, role);
        return membre;
    }

    private MembreSite creerMembreSite(String matricule, String prenom, String nom, String email, Role role, Site site) {
        MembreSite membre = new MembreSite();
        renseignerMembre(membre, matricule, prenom, nom, email, role);
        membre.setSite(site);
        return membre;
    }

    private MembreLibre creerMembreLibre(String matricule, String prenom, String nom, String email) {
        MembreLibre membre = new MembreLibre();
        renseignerMembre(membre, matricule, prenom, nom, email, Role.USER);
        return membre;
    }

    private void renseignerMembre(
            Membre membre,
            String matricule,
            String prenom,
            String nom,
            String email,
            Role role
    ) {
        membre.setMatricule(matricule);
        membre.setPrenom(prenom);
        membre.setNom(nom);
        membre.setEmail(email);
        membre.setRole(role);
    }
}
