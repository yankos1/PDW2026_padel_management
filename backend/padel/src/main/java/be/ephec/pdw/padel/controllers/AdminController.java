package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.AdminDashboardDto;
import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.service.CurrentUserService;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_GLOBAL')")
public class AdminController {
    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    @GetMapping("/matchs")
    public long nombreMatchs() {
        return adminService.nombreMatchs(currentMatricule());
    }

    @GetMapping("/ca")
    public double chiffreAffaires() {
        return adminService.chiffreAffaires(currentMatricule());
    }

    @GetMapping("/membres")
    public long nombreMembres() {
        return adminService.nombreMembres(currentMatricule());
    }

    @GetMapping("/terrains")
    public long nombreTerrains() {
        return adminService.nombreTerrains(currentMatricule());
    }

    @GetMapping("/matchs-complets")
    public long matchsComplets() {
        return adminService.matchsComplets(currentMatricule());
    }

    @GetMapping("/taux-remplissage")
    public double tauxRemplissage() {
        return adminService.tauxRemplissage(currentMatricule());
    }

    @GetMapping("/revenus-par-site")
    public Map<String, Double> revenusParSite() {
        return adminService.revenusParSite(currentMatricule());
    }

    @GetMapping("/sites")
    public List<Site> sites() {
        return adminService.sites(currentMatricule());
    }

    @GetMapping("/terrains-accessibles")
    public List<TerrainDTO> terrains(@RequestParam(required = false) Long siteId) {
        return adminService.terrains(currentMatricule(), siteId);
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long terrainId
    ) {
        return adminService.dashboard(currentMatricule(), dateDebut, dateFin, siteId, terrainId);
    }

    private String currentMatricule() {
        return currentUserService.getCurrentUser().getMatricule();
    }
}
