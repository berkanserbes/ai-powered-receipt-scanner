package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.dto.request.LoginRequest;
import com.berkan.receiptscanner.dto.request.RefreshTokenRequest;
import com.berkan.receiptscanner.dto.request.RegisterRequest;
import com.berkan.receiptscanner.dto.response.LoginResponse;
import com.berkan.receiptscanner.dto.response.RegisterResponse;
import com.berkan.receiptscanner.entity.RefreshToken;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.exception.DuplicateResourceException;
import com.berkan.receiptscanner.repository.UserRepository;
import com.berkan.receiptscanner.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "encoded_password", Role.USER);
        testUser.setId(1L);
    }

    // ========== REGISTER ==========

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Should register a new user successfully")
        void shouldRegisterNewUser() {
            RegisterRequest request = new RegisterRequest("newuser", "password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

            RegisterResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("newuser", response.username());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should not allow registration with an existing username")
        void shouldThrowWhenUsernameAlreadyExists() {
            RegisterRequest request = new RegisterRequest("existinguser", "password123");
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> authService.register(request));

            verify(userRepository, never()).save(any());
        }
    }

    // ========== LOGIN ==========

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Should login with valid credentials")
        void shouldLoginWithValidCredentials() {
            LoginRequest request = new LoginRequest("testuser", "password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.getAccessTokenExpirationInMinutes()).thenReturn(15L);
            when(refreshTokenService.createRefreshToken(testUser))
                    .thenReturn(new RefreshToken("refresh-token", testUser, Instant.now().plusSeconds(3600)));
            when(refreshTokenService.getRefreshTokenExpirationInMinutes()).thenReturn(10080L);

            LoginResponse response = authService.login(request);

                assertAll("Login response assertions",
                    () -> assertNotNull(response),
                    () -> assertEquals("access-token", response.accessToken()),
                    () -> assertEquals("refresh-token", response.refreshToken()),
                    () -> assertEquals("Bearer", response.tokenType()),
                    () -> assertEquals(15L, response.accessTokenExpiresInMinutes()),
                    () -> assertEquals(10080L, response.refreshTokenExpiresInMinutes())
            );

            verify(refreshTokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Should not login with wrong password")
        void shouldThrowOnBadCredentials() {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request));
        }
    }

    // ========== REFRESH TOKEN ==========

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should issue a new token pair with a valid refresh token")
        void shouldRefreshWithValidToken() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            RefreshToken oldToken = new RefreshToken("valid-refresh-token", testUser, Instant.now().plusSeconds(3600));
            RefreshToken newToken = new RefreshToken("new-refresh-token", testUser, Instant.now().plusSeconds(3600));

            when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(oldToken);
            when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
            when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
            when(jwtService.getAccessTokenExpirationInMinutes()).thenReturn(15L);
            when(refreshTokenService.createRefreshToken(testUser)).thenReturn(newToken);
            when(refreshTokenService.getRefreshTokenExpirationInMinutes()).thenReturn(10080L);

            LoginResponse response = authService.refreshToken(request);

                assertAll("Refresh token rotation assertions",
                    () -> assertEquals("new-access-token", response.accessToken()),
                    () -> assertEquals("new-refresh-token", response.refreshToken())
            );

            verify(refreshTokenService).revokeToken(oldToken);
        }
    }
}
