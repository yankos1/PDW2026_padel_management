package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MembreRepository membreRepository;

    @PostMapping("/login")
    public Membre login(@RequestBody LoginDTO input) {
        return membreRepository.findById(input.getMatricule())
                .orElseThrow(() -> new BusinessRuleException("Invalid matricule"));
    }
}
