package be.ephec.pdw.padel.security;

import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
public class CustomUserDetailsService implements UserDetailsService {
    private final MembreRepository membreRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String matricule = normalize(username);
        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));

        Role role = membre.getRole();
        if (role == null) {
            throw new UsernameNotFoundException("Identifiants invalides");
        }

        return new User(
                membre.getMatricule(),
                passwordFor(membre),
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private String passwordFor(Membre membre) {
        if (membre.getRole() == Role.ADMIN_GLOBAL || membre.getRole() == Role.ADMIN_SITE) {
            String hash = membre.getAdminPasswordHash();
            if (hash == null || hash.isBlank()) {
                throw new UsernameNotFoundException("Identifiants invalides");
            }
            return hash;
        }

        throw new UsernameNotFoundException("Identifiants invalides");
    }

    private String normalize(String username) {
        if (username.isBlank()) {
            throw new UsernameNotFoundException("Identifiants invalides");
        }
        return username.trim().toUpperCase();
    }
}
