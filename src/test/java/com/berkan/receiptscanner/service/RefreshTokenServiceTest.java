package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.entity.RefreshToken;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.exception.InvalidRequestException;
import com.berkan.receiptscanner.exception.TokenExpiredException;
import com.berkan.receiptscanner.exception.TokenRevokedException;
import com.berkan.receiptscanner.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMinutes", 10080L);
        testUser = new User("testuser", "encoded_password", Role.USER);
        testUser.setId(1L);
    }

    // ========== CREATE ==========

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("Should create and save a new refresh token")
        void shouldCreateAndSaveToken() {
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(testUser);

                assertAll("Token creation assertions",
                    () -> assertNotNull(result),
                    () -> assertNotNull(result.getToken()),
                    () -> assertFalse(result.getToken().isBlank()),
                    () -> assertEquals(testUser, result.getUser()),
                    () -> assertTrue(result.getExpiryDate().isAfter(Instant.now()))
            );

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    // ========== VERIFY ==========

    @Nested
    @DisplayName("verifyExpiration()")
    class VerifyExpiration {

        @Test
        @DisplayName("Should return the token when it is valid")
        void shouldReturnValidToken() {
            RefreshToken token = new RefreshToken("valid-token", testUser, Instant.now().plusSeconds(3600));

            RefreshToken result = refreshTokenService.verifyExpiration(token);

            assertEquals(token, result);
        }

        @Test
        @DisplayName("Should delete and throw for an expired token")
        void shouldThrowAndDeleteExpiredToken() {
            RefreshToken token = new RefreshToken("expired-token", testUser, Instant.now().minusSeconds(3600));

            assertThrows(TokenExpiredException.class, () -> refreshTokenService.verifyExpiration(token));

            verify(refreshTokenRepository).delete(token);
        }

        @Test
        @DisplayName("Should throw for a revoked token")
        void shouldThrowForRevokedToken() {
            RefreshToken token = new RefreshToken("revoked-token", testUser, Instant.now().plusSeconds(3600));
            token.revoke();

            assertThrows(TokenRevokedException.class, () -> refreshTokenService.verifyExpiration(token));
        }
    }

    // ========== FIND ==========

    @Nested
    @DisplayName("findByToken()")
    class FindByToken {

        @Test
        @DisplayName("Should return an existing token")
        void shouldReturnExistingToken() {
            RefreshToken token = new RefreshToken("existing-token", testUser, Instant.now().plusSeconds(3600));
            when(refreshTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.findByToken("existing-token");

            assertEquals(token, result);
        }

        @Test
        @DisplayName("Should throw for a non-existing token")
        void shouldThrowForInvalidToken() {
            when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            assertThrows(InvalidRequestException.class, () -> refreshTokenService.findByToken("invalid-token"));
        }
    }

    // ========== REVOKE ==========

    @Nested
    @DisplayName("revokeToken()")
    class RevokeToken {

        @Test
        @DisplayName("Should revoke and save the token")
        void shouldRevokeAndSaveToken() {
            RefreshToken token = new RefreshToken("token-to-revoke", testUser, Instant.now().plusSeconds(3600));

            refreshTokenService.revokeToken(token);

            assertTrue(token.isRevoked());
            verify(refreshTokenRepository).save(token);
        }
    }
}
