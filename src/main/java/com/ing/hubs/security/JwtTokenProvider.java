package com.ing.hubs.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  private Key secretKey;

  @PostConstruct
  public void init() {
    this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(String username, String role) {

    return Jwts.builder()
        .setSubject(username)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  public String getUsernameFromToken(String token) {

    return getClaims(token).getSubject();
  }

  public boolean validateToken(String token) {

    try {
      getClaims(token);

      return true;
    } catch (Exception e) {

      return false;
    }
  }

  private Claims getClaims(String token) {

    return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
  }
}
