package com.ing.hubs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  @InjectMocks private JwtTokenProvider jwtTokenProvider;

  private final String secretKey =
      "mySuperSecretKeyThatIsAtLeast64BytesLongForHS512Algorithm12345678901234567890";
  private final long expiration = 3600000;

  @BeforeEach
  void setUp() {

    ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secretKey);
    ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", expiration);
    jwtTokenProvider.init();
  }

  @Test
  void generateToken_WithValidUsernameAndRole_ShouldReturnValidToken() {

    String username = "testUser";
    String role = "ADMIN";

    String token = jwtTokenProvider.generateToken(username, role);

    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
  }

  @Test
  void generateToken_WithDifferentRoles_ShouldIncludeRoleInToken() {

    String username = "testUser";
    String adminRole = "ADMIN";
    String customerRole = "CUSTOMER";

    String adminToken = jwtTokenProvider.generateToken(username, adminRole);
    String customerToken = jwtTokenProvider.generateToken(username, customerRole);

    assertThat(adminToken).isNotNull();
    assertThat(customerToken).isNotNull();
    assertThat(adminToken).isNotEqualTo(customerToken);
  }

  @Test
  void generateToken_WithEmptyUsername_ShouldCreateTokenWithNullSubject() {

    String emptyUsername = "";
    String role = "ADMIN";

    String token = jwtTokenProvider.generateToken(emptyUsername, role);
    String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(extractedUsername).isNull();
  }

  @Test
  void generateToken_WithNullUsername_ShouldCreateTokenWithNullSubject() {

    String nullUsername = null;
    String role = "ADMIN";

    String token = jwtTokenProvider.generateToken(nullUsername, role);
    String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(extractedUsername).isNull();
  }

  @Test
  void generateToken_WithEmptyRole_ShouldCreateToken() {

    String username = "testUser";
    String emptyRole = "";

    String token = jwtTokenProvider.generateToken(username, emptyRole);

    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
  }

  @Test
  void generateToken_WithNullRole_ShouldCreateToken() {

    String username = "testUser";
    String nullRole = null;

    String token = jwtTokenProvider.generateToken(username, nullRole);

    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
  }

  @Test
  void getUsernameFromToken_WithValidToken_ShouldReturnUsername() {

    String username = "testUser";
    String role = "ADMIN";
    String token = jwtTokenProvider.generateToken(username, role);

    String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    assertThat(extractedUsername).isEqualTo(username);
  }

  @Test
  void getUsernameFromToken_WithInvalidToken_ShouldThrowException() {

    String invalidToken = "invalid.token.here";

    assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(invalidToken))
        .isInstanceOf(Exception.class);
  }

  @Test
  void getUsernameFromToken_WithMalformedToken_ShouldThrowException() {

    String malformedToken = "malformed.token";

    assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(malformedToken))
        .isInstanceOf(Exception.class);
  }

  @Test
  void getUsernameFromToken_WithEmptyToken_ShouldThrowException() {

    String emptyToken = "";

    assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(emptyToken))
        .isInstanceOf(Exception.class);
  }

  @Test
  void getUsernameFromToken_WithNullToken_ShouldThrowException() {

    String nullToken = null;

    assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(nullToken))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateToken_WithValidToken_ShouldReturnTrue() {

    String username = "testUser";
    String role = "ADMIN";
    String token = jwtTokenProvider.generateToken(username, role);

    boolean isValid = jwtTokenProvider.validateToken(token);

    assertThat(isValid).isTrue();
  }

  @Test
  void validateToken_WithInvalidSignature_ShouldReturnFalse() {

    String invalidSecret =
        "differentSuperSecretKeyThatIsAtLeast64BytesLongForHS512Algorithm12345678901234567890";
    Key invalidKey = Keys.hmacShaKeyFor(invalidSecret.getBytes(StandardCharsets.UTF_8));

    String token =
        Jwts.builder()
            .setSubject("testUser")
            .claim("role", "ADMIN")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(invalidKey, SignatureAlgorithm.HS512)
            .compact();

    boolean isValid = jwtTokenProvider.validateToken(token);

    assertThat(isValid).isFalse();
  }

  @Test
  void validateToken_WithExpiredToken_ShouldReturnFalse() {

    Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    String expiredToken =
        Jwts.builder()
            .setSubject("testUser")
            .claim("role", "ADMIN")
            .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();

    boolean isValid = jwtTokenProvider.validateToken(expiredToken);

    assertThat(isValid).isFalse();
  }

  @Test
  void validateToken_WithMalformedToken_ShouldReturnFalse() {

    String malformedToken = "malformed.token.string";

    boolean isValid = jwtTokenProvider.validateToken(malformedToken);

    assertThat(isValid).isFalse();
  }

  @Test
  void validateToken_WithEmptyToken_ShouldReturnFalse() {

    String emptyToken = "";

    boolean isValid = jwtTokenProvider.validateToken(emptyToken);

    assertThat(isValid).isFalse();
  }

  @Test
  void validateToken_WithNullToken_ShouldReturnFalse() {

    String nullToken = null;

    boolean isValid = jwtTokenProvider.validateToken(nullToken);

    assertThat(isValid).isFalse();
  }

  @Test
  void init_WithValidSecretKey_ShouldInitializeSecretKey() {

    JwtTokenProvider newProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(newProvider, "jwtSecret", secretKey);
    ReflectionTestUtils.setField(newProvider, "jwtExpiration", expiration);

    newProvider.init();

    assertThat(newProvider).isNotNull();
  }

  @Test
  void init_WithShortSecretKey_ShouldThrowException() {

    JwtTokenProvider newProvider = new JwtTokenProvider();
    String shortSecret = "shortKey"; // Too short for HS512
    ReflectionTestUtils.setField(newProvider, "jwtSecret", shortSecret);
    ReflectionTestUtils.setField(newProvider, "jwtExpiration", expiration);

    assertThatThrownBy(newProvider::init).isInstanceOf(WeakKeyException.class);
  }
}
