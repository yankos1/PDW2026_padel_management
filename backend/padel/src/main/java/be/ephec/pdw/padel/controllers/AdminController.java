package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.service.CurrentUserService;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private String currentMatricule() {
        return currentUserService.getCurrentUser().getMatricule();
    }
}
