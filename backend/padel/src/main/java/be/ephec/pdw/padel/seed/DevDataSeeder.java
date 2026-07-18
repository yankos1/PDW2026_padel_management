package be.ephec.pdw.padel.seed;

import be.ephec.pdw.padel.constants.BusinessConstants;
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
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {
    private static final double PRIX_RESERVATION_PAYEE = BusinessConstants.MATCH_PRICE_PER_PLAYER;

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
    public void run(String @NonNull ... args) {
        SitesEtTerrains sites = seedSitesTerrainsEtFermetures();
        MembresDemo membres = seedMembres(sites.bruxelles());
        seedSoldesEtPenalites(membres);
        seedMatchsHistoriques(sites, membres);
        seedMatchsFuturs(sites, membres);
        log.info("Seed dev termine: donnees de demonstration coherentes et idempotentes disponibles");
    }

    private SitesEtTerrains seedSitesTerrainsEtFermetures() {
        Site bruxelles = getOrCreateSite("Padel Bruxelles", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Site wavre = getOrCreateSite("Padel Wavre", LocalTime.of(9, 0), LocalTime.of(21, 0));

        Terrain bruxelles1 = getOrCreateTerrain("Bruxelles 1", bruxelles);
        Terrain bruxelles2 = getOrCreateTerrain("Bruxelles 2", bruxelles);
        Terrain wavre1 = getOrCreateTerrain("Wavre 1", wavre);
        Terrain wavre2 = getOrCreateTerrain("Wavre 2", wavre);

        YearMonth prochainMois = YearMonth.now().plusMonths(1);
        getOrCreateJourFermeture(bruxelles, prochainMois.atDay(10));
        getOrCreateJourFermeture(wavre, prochainMois.atDay(15));
        getOrCreateJourFermetureGlobale(prochainMois.atDay(20));

        log.info("Seed dev: sites, terrains et fermetures prets");
        return new SitesEtTerrains(bruxelles, wavre, bruxelles1, bruxelles2, wavre1, wavre2);
    }

    private MembresDemo seedMembres(Site bruxelles) {
        MembreGlobal adminGlobal = getOrCreateMembreGlobal("G0001", "Admin", "Global", "admin.global@example.test", Role.ADMIN_GLOBAL);
        MembreGlobal adminTest1 = getOrCreateMembreGlobal("G0101", "Administrateur", "Test", "admin.test@padel.local", Role.ADMIN_GLOBAL);
        MembreGlobal adminTest2 = getOrCreateMembreGlobal("G0102", "Administrateur Deux", "Test", "admin.test2@padel.local", Role.ADMIN_GLOBAL);
        initialiserMotDePasseAdmin(adminGlobal);
        initialiserMotDePasseAdmin(adminTest1);
        initialiserMotDePasseAdmin(adminTest2);

        MembreGlobal alice = getOrCreateMembreGlobal("G0002", "Alice", "Martin", "alice.martin@example.test", Role.USER);
        MembreGlobal emma = getOrCreateMembreGlobal("G0003", "Emma", "Bernard", "emma.bernard@example.test", Role.USER);
        MembreGlobal lucas = getOrCreateMembreGlobal("G0004", "Lucas", "Petit", "lucas.petit@example.test", Role.USER);
        MembreGlobal mila = getOrCreateMembreGlobal("G0005", "Mila", "Robert", "mila.robert@example.test", Role.USER);
        MembreGlobal adam = getOrCreateMembreGlobal("G0006", "Adam", "Lefevre", "adam.lefevre@example.test", Role.USER);

        MembreSite adminSite = getOrCreateMembreSite("S0001", "Admin", "Site", "admin.site@example.test", Role.ADMIN_SITE, bruxelles);
        initialiserMotDePasseAdmin(adminSite);
        MembreSite bruno = getOrCreateMembreSite("S0002", "Bruno", "Lambert", "bruno.lambert@example.test", Role.USER, bruxelles);
        MembreSite nora = getOrCreateMembreSite("S0003", "Nora", "Dubois", "nora.dubois@example.test", Role.USER, bruxelles);
        MembreSite hugo = getOrCreateMembreSite("S0004", "Hugo", "Moreau", "hugo.moreau@example.test", Role.USER, bruxelles);
        MembreSite lea = getOrCreateMembreSite("S0005", "Lea", "Simon", "lea.simon@example.test", Role.USER, bruxelles);
        MembreSite inesSite = getOrCreateMembreSite("S0006", "Ines", "Vermeulen", "ines.vermeulen@example.test", Role.USER, bruxelles);

        MembreLibre chloe = getOrCreateMembreLibre("L0001", "Chloe", "Durand", "chloe.durand@example.test");
        MembreLibre noah = getOrCreateMembreLibre("L0002", "Noah", "Leroy", "noah.leroy@example.test");
        MembreLibre ines = getOrCreateMembreLibre("L0003", "Ines", "Roux", "ines.roux@example.test");
        MembreLibre louis = getOrCreateMembreLibre("L0004", "Louis", "Fournier", "louis.fournier@example.test");
        MembreLibre zoe = getOrCreateMembreLibre("L0005", "Zoe", "Girard", "zoe.girard@example.test");

        log.info("Seed dev: membres et administrateurs prets");
        return new MembresDemo(adminGlobal, adminTest1, adminTest2, adminSite, alice, emma, lucas, mila, adam,
                bruno, nora, hugo, lea, inesSite, chloe, noah, ines, louis, zoe);
    }

    private void seedSoldesEtPenalites(MembresDemo membres) {
        definirSoldeEtPenalite(membres.bruno(), 15.0, false, null);
        definirSoldeEtPenalite(membres.nora(), 30.0, false, null);
        definirSoldeEtPenalite(membres.hugo(), 0.0, true, LocalDateTime.now().plusDays(6).withSecond(0).withNano(0));
        definirSoldeEtPenalite(membres.lea(), 0.0, true, LocalDateTime.now().minusDays(2).withSecond(0).withNano(0));
        definirSoldeEtPenalite(membres.alice(), 0.0, false, null);
        log.info("Seed dev: soldes et penalites prets");
    }

    private void seedMatchsHistoriques(SitesEtTerrains sites, MembresDemo membres) {
        creerMatchComplet(
                sites.bruxelles1(),
                membres.alice(),
                dateHistorique(0, 5, 10, 0),
                true,
                StatutMatch.TERMINE,
                membres.alice(), membres.emma(), membres.lucas(), membres.chloe()
        );
        creerMatchComplet(
                sites.wavre1(),
                membres.emma(),
                dateHistorique(0, 12, 11, 45),
                false,
                StatutMatch.TERMINE,
                membres.emma(), membres.mila(), membres.noah(), membres.ines()
        );
        Match bruxellesPublicRecent = getOrCreateMatchExact(
                membres.lucas(),
                sites.bruxelles2(),
                dateHistorique(0, 15, 18, 0),
                true,
                StatutMatch.TERMINE
        );
        getOrCreateReservationPayee(bruxellesPublicRecent, membres.lucas());
        getOrCreateReservationPayee(bruxellesPublicRecent, membres.mila());
        getOrCreateReservationPayee(bruxellesPublicRecent, membres.louis());

        Match wavreAnnule = getOrCreateMatchExact(
                membres.mila(),
                sites.wavre2(),
                dateHistorique(1, 10, 17, 0),
                false,
                StatutMatch.ANNULE
        );
        getOrCreateReservationAnnulee(wavreAnnule, membres.mila());

        creerMatchComplet(
                sites.bruxelles1(),
                membres.adam(),
                dateHistorique(2, 8, 9, 45),
                true,
                StatutMatch.TERMINE,
                membres.adam(), membres.alice(), membres.zoe(), membres.ines()
        );
        creerMatchComplet(
                sites.wavre1(),
                membres.chloe(),
                dateHistorique(2, 22, 15, 15),
                true,
                StatutMatch.TERMINE,
                membres.chloe(), membres.noah(), membres.louis(), membres.zoe()
        );
        log.info("Seed dev: matchs historiques prets");
    }

    private void seedMatchsFuturs(SitesEtTerrains sites, MembresDemo membres) {
        Match publicDisponible = getOrCreateMatchExact(
                membres.alice(),
                sites.bruxelles1(),
                dateDans(3, 10, 0),
                true,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(publicDisponible, membres.alice());
        getOrCreateReservationPayee(publicDisponible, membres.chloe());

        Match publicPresqueComplet = getOrCreateMatchExact(
                membres.emma(),
                sites.wavre1(),
                dateDans(4, 10, 0),
                true,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(publicPresqueComplet, membres.emma());
        getOrCreateReservationPayee(publicPresqueComplet, membres.lucas());
        getOrCreateReservationPayee(publicPresqueComplet, membres.noah());

        creerMatchComplet(
                sites.wavre2(),
                membres.mila(),
                dateDans(5, 11, 0),
                true,
                StatutMatch.COMPLET,
                membres.mila(), membres.alice(), membres.ines(), membres.louis()
        );

        Match priveIncomplet = getOrCreateMatchExact(
                membres.bruno(),
                sites.bruxelles2(),
                dateDans(4, 14, 0),
                false,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(priveIncomplet, membres.bruno());
        getOrCreateReservationPayee(priveIncomplet, membres.inesSite());
        getOrCreateReservationEnAttente(priveIncomplet, membres.nora());
        getOrCreateReservationEnAttente(priveIncomplet, membres.hugo());

        Match procheConversion = getOrCreateMatchExact(
                membres.lea(),
                sites.bruxelles1(),
                dateDans(2, 12, 0),
                false,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(procheConversion, membres.lea());
        getOrCreateReservationEnAttente(procheConversion, membres.inesSite());

        Match wavrePriveFiltre = getOrCreateMatchExact(
                membres.adam(),
                sites.wavre2(),
                dateDans(6, 16, 0),
                false,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(wavrePriveFiltre, membres.adam());
        getOrCreateReservationPayee(wavrePriveFiltre, membres.zoe());
        getOrCreateReservationAnnulee(wavrePriveFiltre, membres.noah());

        Match bruxellesPublicSoir = getOrCreateMatchExact(
                membres.lucas(),
                sites.bruxelles2(),
                dateDans(7, 19, 30),
                true,
                StatutMatch.PLANIFIE
        );
        getOrCreateReservationPayee(bruxellesPublicSoir, membres.lucas());

        log.info("Seed dev: matchs futurs prets");
    }

    private Site getOrCreateSite(String name, LocalTime ouverture, LocalTime fermeture) {
        Site existing = findSiteByName(name);

        if (existing != null) {
            boolean changed = false;
            if (!ouverture.equals(existing.getHeureOuverture())) {
                existing.setHeureOuverture(ouverture);
                changed = true;
            }
            if (!fermeture.equals(existing.getHeureFermeture())) {
                existing.setHeureFermeture(fermeture);
                changed = true;
            }
            return changed ? siteRepository.save(existing) : existing;
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

    private void getOrCreateJourFermetureGlobale(LocalDate date) {
        if (!jourFermetureRepository.existsBySiteIsNullAndDate(date)) {
            jourFermetureRepository.save(JourFermeture.builder()
                    .site(null)
                    .date(date)
                    .build());
        }
    }

    private MembreGlobal getOrCreateMembreGlobal(String matricule, String prenom, String nom, String email, Role role) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        if (membre instanceof MembreGlobal membreGlobal) {
            return membreGlobal;
        }

        return membreRepository.save(creerMembreGlobal(matricule, prenom, nom, email, role));
    }

    private MembreSite getOrCreateMembreSite(String matricule, String prenom, String nom, String email, Role role, Site site) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        if (membre instanceof MembreSite membreSite) {
            if (membreSite.getSite() == null || !Objects.equals(membreSite.getSite().getId(), site.getId())) {
                membreSite.setSite(site);
                return membreRepository.save(membreSite);
            }
            return membreSite;
        }

        return membreRepository.save(creerMembreSite(matricule, prenom, nom, email, role, site));
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

    private MembreLibre getOrCreateMembreLibre(String matricule, String prenom, String nom, String email) {
        Membre membre = membreRepository.findById(matricule).orElse(null);
        if (membre instanceof MembreLibre membreLibre) {
            return membreLibre;
        }

        return membreRepository.save(creerMembreLibre(matricule, prenom, nom, email));
    }

    private void definirSoldeEtPenalite(Membre membre, double soldeDu, boolean penaliteActive, LocalDateTime finPenalite) {
        boolean changed = false;
        if (Double.compare(membre.getSoldeDu(), soldeDu) != 0) {
            membre.setSoldeDu(soldeDu);
            changed = true;
        }
        if (membre.isPenaliteActive() != (penaliteActive && finPenalite != null && finPenalite.isAfter(LocalDateTime.now()))) {
            membre.setPenaliteActive(penaliteActive);
            changed = true;
        }
        if (!Objects.equals(membre.getFinPenalite(), finPenalite)) {
            membre.setFinPenalite(finPenalite);
            changed = true;
        }
        if (changed) {
            membreRepository.save(membre);
        }
    }

    private void creerMatchComplet(
            Terrain terrain,
            Membre organisateur,
            LocalDateTime dateHeureDebut,
            boolean estPublic,
            StatutMatch statut,
            Membre joueur1,
            Membre joueur2,
            Membre joueur3,
            Membre joueur4
    ) {
        Match match = getOrCreateMatchExact(organisateur, terrain, dateHeureDebut, estPublic, statut);
        getOrCreateReservationPayee(match, joueur1);
        getOrCreateReservationPayee(match, joueur2);
        getOrCreateReservationPayee(match, joueur3);
        getOrCreateReservationPayee(match, joueur4);
    }

    private Match getOrCreateMatchExact(
            Membre organisateur,
            Terrain terrain,
            LocalDateTime dateHeureDebut,
            boolean estPublic,
            StatutMatch statut
    ) {
        return matchRepository.findByTerrain(terrain)
                .stream()
                .filter(match -> dateHeureDebut.equals(match.getDateHeureDebut()))
                .filter(match -> match.getOrganisateur() != null)
                .filter(match -> organisateur.getMatricule().equals(match.getOrganisateur().getMatricule()))
                .filter(match -> match.isEstPublic() == estPublic)
                .findFirst()
                .map(match -> mettreAJourStatutSeed(match, statut))
                .orElseGet(() -> findMatchSeedFuturARafraichir(organisateur, terrain, dateHeureDebut, estPublic)
                        .map(match -> rafraichirMatchSeed(match, dateHeureDebut, estPublic, statut))
                        .orElseGet(() -> matchRepository.save(creerMatch(organisateur, terrain, dateHeureDebut, estPublic, statut))));
    }

    private java.util.Optional<Match> findMatchSeedFuturARafraichir(
            Membre organisateur,
            Terrain terrain,
            LocalDateTime nouvelleDate,
            boolean estPublic
    ) {
        LocalDateTime limiteBasse = LocalDateTime.now().minusDays(7);
        LocalDateTime limiteHaute = LocalDateTime.now().plusDays(21);
        List<Match> candidats = matchRepository.findByTerrain(terrain)
                .stream()
                .filter(match -> match.getOrganisateur() != null)
                .filter(match -> organisateur.getMatricule().equals(match.getOrganisateur().getMatricule()))
                .filter(match -> match.isEstPublic() == estPublic || !estPublic)
                .filter(match -> match.getStatut() != StatutMatch.TERMINE)
                .filter(match -> match.getStatut() != StatutMatch.ANNULE)
                .filter(match -> match.getDateHeureDebut() != null)
                .filter(match -> match.getDateHeureDebut().toLocalTime().equals(nouvelleDate.toLocalTime()))
                .filter(match -> !match.getDateHeureDebut().equals(nouvelleDate))
                .filter(match -> !match.getDateHeureDebut().isBefore(limiteBasse))
                .filter(match -> !match.getDateHeureDebut().isAfter(limiteHaute))
                .toList();

        if (candidats.size() != 1 || matchRepository.existsByTerrainAndDateHeureDebut(terrain, nouvelleDate)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(candidats.getFirst());
    }

    private Match rafraichirMatchSeed(Match match, LocalDateTime dateHeureDebut, boolean estPublic, StatutMatch statut) {
        match.setDateHeureDebut(dateHeureDebut);
        match.setEstPublic(estPublic);
        match.setStatut(statut);
        return matchRepository.save(match);
    }

    private Match mettreAJourStatutSeed(Match match, StatutMatch statut) {
        if (match.getStatut() != statut) {
            match.setStatut(statut);
            return matchRepository.save(match);
        }
        return match;
    }

    private void getOrCreateReservationPayee(Match match, Membre membre) {
        getOrCreateReservation(match, membre, true, PRIX_RESERVATION_PAYEE, LocalDateTime.now(), StatutReservation.CONFIRMEE);
    }

    private void getOrCreateReservationEnAttente(Match match, Membre membre) {
        getOrCreateReservation(match, membre, false, 0.0, null, StatutReservation.EN_ATTENTE);
    }

    private void getOrCreateReservationAnnulee(Match match, Membre membre) {
        getOrCreateReservation(match, membre, false, 0.0, null, StatutReservation.ANNULEE);
    }

    private void getOrCreateReservation(
            Match match,
            Membre membre,
            boolean estPayee,
            double montant,
            LocalDateTime datePaiement,
            StatutReservation statut
    ) {
        Reservation reservation = match.getReservations() == null ? null : match.getReservations()
                .stream()
                .filter(existing -> existing.getMembre() != null)
                .filter(existing -> membre.getMatricule().equals(existing.getMembre().getMatricule()))
                .findFirst()
                .orElse(null);

        if (reservation == null) {
            reservation = Reservation.builder()
                    .match(match)
                    .membre(membre)
                    .dateReservation(LocalDateTime.now())
                    .build();
        }

        reservation.setEstPayee(estPayee);
        reservation.setMontant(montant);
        if (estPayee && reservation.getDatePaiement() == null) {
            reservation.setDatePaiement(normalizedNow(datePaiement));
        }
        if (!estPayee) {
            reservation.setDatePaiement(null);
        }
        reservation.setStatut(statut);
        reservationRepository.save(reservation);
    }

    private LocalDateTime normalizedNow(LocalDateTime value) {
        return value == null ? null : value.withSecond(0).withNano(0);
    }

    private Match creerMatch(Membre organisateur, Terrain terrain, LocalDateTime dateHeureDebut, boolean estPublic, StatutMatch statut) {
        return Match.builder()
                .organisateur(organisateur)
                .terrain(terrain)
                .dateHeureDebut(dateHeureDebut)
                .estPublic(estPublic)
                .statut(statut)
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

    private LocalDateTime dateHistorique(int moisEnArriere, int jourDuMois, int heure, int minute) {
        YearMonth mois = YearMonth.now().minusMonths(moisEnArriere);
        int jour = jourDuMois;
        if (moisEnArriere == 0) {
            jour = Math.clamp(LocalDate.now().minusDays(1).getDayOfMonth(), 1, jourDuMois);
        }
        jour = Math.min(jour, mois.lengthOfMonth());
        return mois.atDay(jour).atTime(heure, minute);
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

    private record SitesEtTerrains(
            Site bruxelles,
            Site wavre,
            Terrain bruxelles1,
            Terrain bruxelles2,
            Terrain wavre1,
            Terrain wavre2
    ) {
    }

    private record MembresDemo(
            MembreGlobal adminGlobal,
            MembreGlobal adminTest1,
            MembreGlobal adminTest2,
            MembreSite adminSite,
            MembreGlobal alice,
            MembreGlobal emma,
            MembreGlobal lucas,
            MembreGlobal mila,
            MembreGlobal adam,
            MembreSite bruno,
            MembreSite nora,
            MembreSite hugo,
            MembreSite lea,
            MembreSite inesSite,
            MembreLibre chloe,
            MembreLibre noah,
            MembreLibre ines,
            MembreLibre louis,
            MembreLibre zoe
    ) {
    }
}
