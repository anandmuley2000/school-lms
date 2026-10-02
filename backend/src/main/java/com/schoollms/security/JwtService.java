package com.schoollms.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }
    this.key = Keys.hmacShaKeyFor(secretBytes);
    this.expirationMs = expirationMs;
  }

  public String issue(UserDetails user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public String subject(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean valid(String token, UserDetails user) {
    try {
      return subject(token).equals(user.getUsername())
          && Jwts.parser()
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getExpiration()
              .after(new Date());
    } catch (Exception e) {
      return false;
    }
  }
}
