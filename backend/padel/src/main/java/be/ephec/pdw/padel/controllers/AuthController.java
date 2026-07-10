package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.dto.SessionDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.security.JwtService;
import be.ephec.pdw.padel.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public SessionDTO login(@Valid @RequestBody LoginDTO input) {
        Membre membre = authService.login(input);
        return SessionDTO.from(membre, jwtService.generateToken(membre));
    }

    @GetMapping("/admin-password-status/{matricule}")
    public Map<String, Boolean> adminPasswordStatus(@PathVariable String matricule) {
        return authService.adminPasswordStatus(matricule);
    }

    @PostMapping("/register")
    public SessionDTO register(@Valid @RequestBody RegisterDTO input) {
        Membre membre = authService.register(input);
        return SessionDTO.from(membre, jwtService.generateToken(membre));
    }
}
