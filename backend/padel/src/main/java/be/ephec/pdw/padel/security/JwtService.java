package be.ephec.pdw.padel.security;

import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 octets");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Membre membre) {
        if (membre.getRole() == null) {
            throw new IllegalArgumentException("Role utilisateur manquant");
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(membre.getMatricule())
                .claim("role", membre.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<JwtUser> validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String matricule = claims.getSubject();
            String roleClaim = claims.get("role", String.class);

            if (matricule == null || matricule.isBlank() || roleClaim == null || roleClaim.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new JwtUser(matricule, Role.valueOf(roleClaim)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record JwtUser(String matricule, Role role) {
    }
}
