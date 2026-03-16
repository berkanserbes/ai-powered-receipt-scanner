package com.berkan.receiptscanner.security;

import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private static final String TEST_SECRET = "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2Vz";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMinutes", 15L);
        jwtService.init();

        testUser = new User("testuser", "encoded_password", Role.USER);
        testUser.setId(1L);
    }

    // ========== GENERATE ==========

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("Should generate a valid JWT token")
        void shouldGenerateValidToken() {
            String token = jwtService.generateAccessToken(testUser);

                assertAll("Token generation assertions",
                    () -> assertNotNull(token),
                    () -> assertFalse(token.isBlank()),
                    () -> assertEquals(3, token.split("\\.").length, "JWT must have 3 parts (header.payload.signature)")
            );
        }

        @Test
        @DisplayName("Should include correct username in token")
        void shouldContainCorrectUsername() {
            String token = jwtService.generateAccessToken(testUser);
            String username = jwtService.extractUsername(token);

            assertEquals("testuser", username);
        }

        @Test
        @DisplayName("Should include roles in token")
        void shouldContainRoles() {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.validateAndExtractClaims(token, testUser);

            assertNotNull(claims);
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            assertTrue(roles.contains("ROLE_USER"));
        }
    }

    // ========== EXTRACT ==========

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("Should extract correct username from token")
        void shouldExtractUsername() {
            String token = jwtService.generateAccessToken(testUser);

            assertEquals("testuser", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Should throw for invalid token")
        void shouldThrowForInvalidToken() {
            assertThrows(InvalidTokenException.class,
                    () -> jwtService.extractUsername("invalid.token.value"));
        }
    }

    // ========== VALIDATE ==========

    @Nested
    @DisplayName("validateAndExtractClaims()")
    class ValidateAndExtractClaims {

        @Test
        @DisplayName("Should validate valid token and return claims")
        void shouldValidateValidToken() {
            String token = jwtService.generateAccessToken(testUser);

            Claims claims = jwtService.validateAndExtractClaims(token, testUser);

            assertNotNull(claims);
            assertEquals("testuser", claims.getSubject());
        }

        @Test
        @DisplayName("Should return null when validating with different user")
        void shouldReturnNullForDifferentUser() {
            String token = jwtService.generateAccessToken(testUser);

            User otherUser = new User("otheruser", "password", Role.USER);

            Claims claims = jwtService.validateAndExtractClaims(token, otherUser);

            assertNull(claims);
        }

        @Test
        @DisplayName("Should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            // Set expiration to -1 minute for token creation.
            ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMinutes", -1L);

            // Create an already expired token directly.
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            String expiredToken = Jwts.builder()
                    .claims(Map.of("roles", List.of("ROLE_USER")))
                    .subject("testuser")
                    .issuedAt(new Date(System.currentTimeMillis() - 120000))
                    .expiration(new Date(System.currentTimeMillis() - 60000))
                    .signWith(key)
                    .compact();

            Claims claims = jwtService.validateAndExtractClaims(expiredToken, testUser);

            assertNull(claims);
        }
    }
}
