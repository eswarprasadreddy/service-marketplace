package com.servicehub.config;

import com.servicehub.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secret;

    private Key key() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("Set JWT_SECRET to at least 32 characters in backend/.env.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(User user) {
        Date now = new Date();
        return Jwts.builder().setSubject(user.getId()).claim("id", user.getId()).claim("role", user.getRole())
                .setIssuedAt(now).setExpiration(new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public Claims readToken(String token) { return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody(); }
}
