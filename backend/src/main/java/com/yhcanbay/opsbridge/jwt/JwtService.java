package com.yhcanbay.opsbridge.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

@Component
public class JwtService {

    public static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private static final String ISSUER = "opsbridge";
    private static final String AUDIENCE = "opsbridge-api";

    // Tek backend sürecinde anahtar bellekte kalır ve yeniden başlatmada yenilenir.
    private final SecretKey signingKey = Jwts.SIG.HS256.key().build();
    private final JwtParser parser = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(ISSUER)
            .requireAudience(AUDIENCE)
            .sig().clear().add(Jwts.SIG.HS256).and()
            .build();
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_LIFETIME)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims readActiveToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getId() == null || claims.getExpiration() == null) {
            throw new JwtException("Missing required OpsBridge token claims");
        }
        if (revokedTokens.containsKey(claims.getId())) {
            throw new JwtException("OpsBridge token has been revoked");
        }
        return claims;
    }

    public void revokeToken(Claims claims) {
        Instant now = Instant.now();
        revokedTokens.values().removeIf(expiration -> !expiration.isAfter(now));
        Instant expiration = claims.getExpiration().toInstant();
        if (expiration.isAfter(now)) {
            revokedTokens.put(claims.getId(), expiration);
        }
    }
}
