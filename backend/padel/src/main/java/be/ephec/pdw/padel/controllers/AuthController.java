package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // TODO [IMPORTANT] Mettre en place Spring Security avec JWT pour authentifier les appels API.
    @PostMapping("/login")
    // TODO [IMPORTANT] Remplacer le retour d'entite JPA par un DTO de session minimal.
    public Membre login(@RequestBody LoginDTO input) {
        return authService.login(input);
    }

    @GetMapping("/admin-password-status/{matricule}")
    public Map<String, Boolean> adminPasswordStatus(@PathVariable String matricule) {
        return authService.adminPasswordStatus(matricule);
    }

    @PostMapping("/register")
    // TODO [IMPORTANT] Valider les champs d'inscription avec Bean Validation avant le service.
    public Membre register(@RequestBody RegisterDTO input) {
        return authService.register(input);
    }
}
