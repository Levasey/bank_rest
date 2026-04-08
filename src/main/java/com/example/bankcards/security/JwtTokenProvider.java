package com.example.bankcards.security;

import com.example.bankcards.config.AppSecurityProperties;
import com.example.bankcards.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final AppSecurityProperties appSecurityProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
        this.secretKey =
                Keys.hmacShaKeyFor(appSecurityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(UserAccount user) {
        Date now = new Date();
        long exp = appSecurityProperties.getJwt().getExpirationMs();
        Date expiry = new Date(now.getTime() + exp);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).get("uid", String.class));
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
