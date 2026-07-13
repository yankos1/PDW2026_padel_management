package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.ChangeAdminPasswordDTO;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.dto.SessionDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.security.JwtService;
import be.ephec.pdw.padel.service.AuthService;
import be.ephec.pdw.padel.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public SessionDTO login(@Valid @RequestBody LoginDTO input) {
        Membre membre = authService.login(input);
        return SessionDTO.from(membre, jwtService.generateToken(membre));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeAdminPassword(@Valid @RequestBody ChangeAdminPasswordDTO input) {
        Membre currentUser = currentUserService.getCurrentUser();
        authService.changeAdminPassword(
                currentUser.getMatricule(),
                input.getCurrentPassword(),
                input.getNewPassword(),
                input.getConfirmPassword()
        );
    }

    @PostMapping("/register")
    public SessionDTO register(@Valid @RequestBody RegisterDTO input) {
        Membre membre = authService.register(input);
        return SessionDTO.from(membre, jwtService.generateToken(membre));
    }
}
