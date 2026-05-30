package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/matchs")
    public long nombreMatchs(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.nombreMatchs(matricule);
    }

    @GetMapping("/ca")
    public double chiffreAffaires(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.chiffreAffaires(matricule);
    }

    @GetMapping("/membres")
    public long nombreMembres(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.nombreMembres(matricule);
    }

    @GetMapping("/terrains")
    public long nombreTerrains(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.nombreTerrains(matricule);
    }

    @GetMapping("/matchs-complets")
    public long matchsComplets(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.matchsComplets(matricule);
    }

    @GetMapping("/taux-remplissage")
    public double tauxRemplissage(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.tauxRemplissage(matricule);
    }

    @GetMapping("/revenus-par-site")
    public Map<String, Double> revenusParSite(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.revenusParSite(matricule);
    }

    @GetMapping("/sites")
    public List<Site> sites(@RequestHeader("X-User-Matricule") String matricule) {
        return adminService.sites(matricule);
    }
}
