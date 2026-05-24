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

    @PostMapping("/login")
    public Membre login(@RequestBody LoginDTO input) {
        return authService.login(input);
    }

    @GetMapping("/admin-password-status/{matricule}")
    public Map<String, Boolean> adminPasswordStatus(@PathVariable String matricule) {
        return authService.adminPasswordStatus(matricule);
    }

    @PostMapping("/register")
    public Membre register(@RequestBody RegisterDTO input) {
        return authService.register(input);
    }
}
