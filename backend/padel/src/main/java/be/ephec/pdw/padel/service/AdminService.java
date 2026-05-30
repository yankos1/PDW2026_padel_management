package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.ForbiddenException;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;
    private final SiteRepository siteRepository;

    public long nombreMatchs(String matricule) {
        Membre admin = getAdmin(matricule);
        return matchRepository.findAll().stream()
                .filter(match -> canAccessMatch(admin, match))
                .count();
    }

    public double chiffreAffaires(String matricule) {
        Membre admin = getAdmin(matricule);
        return reservationRepository.findAll().stream()
                .filter(Reservation::isEstPayee)
                .filter(reservation -> canAccessMatch(admin, reservation.getMatch()))
                .mapToDouble(Reservation::getMontant)
                .sum();
    }

    public long nombreMembres(String matricule) {
        Membre admin = getAdmin(matricule);
        return membreRepository.findAll().stream()
                .filter(membre -> canAccessMember(admin, membre))
                .count();
    }

    public long nombreTerrains(String matricule) {
        Membre admin = getAdmin(matricule);
        return terrainRepository.findAll().stream()
                .filter(terrain -> canAccessSite(admin, terrain.getSite()))
                .count();
    }

    public long matchsComplets(String matricule) {
        Membre admin = getAdmin(matricule);
        return matchRepository.findAll().stream()
                .filter(match -> match.getStatut() == StatutMatch.COMPLET)
                .filter(match -> canAccessMatch(admin, match))
                .count();
    }

    public double tauxRemplissage(String matricule) {
        Membre admin = getAdmin(matricule);
        List<Match> matchs = matchRepository.findAll().stream()
                .filter(match -> canAccessMatch(admin, match))
                .toList();

        long totalPlaces = matchs.size() * 4L;
        long reservations = reservationRepository.findAll().stream()
                .filter(reservation -> canAccessMatch(admin, reservation.getMatch()))
                .count();

        if (totalPlaces == 0) return 0;

        return (double) reservations / totalPlaces * 100;
    }

    public Map<String, Double> revenusParSite(String matricule) {
        Membre admin = getAdmin(matricule);
        Map<String, Double> revenus = new HashMap<>();

        for (Reservation reservation : reservationRepository.findAll()) {
            if (reservation.isEstPayee() && canAccessMatch(admin, reservation.getMatch())) {
                Site site = reservation.getMatch()
                        .getTerrain()
                        .getSite();

                String name = site.getName();

                revenus.put(name, revenus.getOrDefault(name, 0.0) + reservation.getMontant());
            }
        }
        return revenus;
    }

    public List<Site> sites(String matricule) {
        Membre admin = getAdmin(matricule);

        if (admin.getRole() == Role.ADMIN_GLOBAL) {
            return siteRepository.findAll();
        }

        if (admin instanceof MembreSite adminSite && adminSite.getSite() != null) {
            return List.of(adminSite.getSite());
        }

        return List.of();
    }

    //verification simplifié des droits admin via le matricule envoyé dans le header HTTP
    private Membre getAdmin(String matricule) {
        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new ForbiddenException("Utilisateur inconnu"));

        if (membre.getRole() != Role.ADMIN_GLOBAL && membre.getRole() != Role.ADMIN_SITE) {
            throw new ForbiddenException("Acces refuse");
        }

        return membre;
    }

    private boolean canAccessMatch(Membre admin, Match match) {
        return match != null
                && match.getTerrain() != null
                && canAccessSite(admin, match.getTerrain().getSite());
    }

    private boolean canAccessMember(Membre admin, Membre membre) {
        if (admin.getRole() == Role.ADMIN_GLOBAL) {
            return true;
        }

        if (!(membre instanceof MembreSite membreSite)) {
            return false;
        }

        return canAccessSite(admin, membreSite.getSite());
    }

    private boolean canAccessSite(Membre admin, Site site) {
        if (admin.getRole() == Role.ADMIN_GLOBAL) {
            return true;
        }

        if (!(admin instanceof MembreSite adminSite)) {
            return false;
        }

        return site != null
                && adminSite.getSite() != null
                && Objects.equals(site.getId(), adminSite.getSite().getId());
    }
}
