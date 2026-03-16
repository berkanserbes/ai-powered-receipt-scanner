package com.berkan.receiptscanner.controller;

import com.berkan.receiptscanner.dto.request.LoginRequest;
import com.berkan.receiptscanner.dto.request.RefreshTokenRequest;
import com.berkan.receiptscanner.dto.request.RegisterRequest;
import com.berkan.receiptscanner.dto.response.LoginResponse;
import com.berkan.receiptscanner.dto.response.RegisterResponse;
import com.berkan.receiptscanner.exception.DuplicateResourceException;
import com.berkan.receiptscanner.filter.JwtAuthenticationFilter;
import com.berkan.receiptscanner.filter.RequestRateLimitFilter;
import com.berkan.receiptscanner.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // Dependencies required by SecurityConfig constructor
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RequestRateLimitFilter requestRateLimitFilter;

    // ========== REGISTER ==========

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("Should return 201 on successful registration")
        void shouldReturn201OnSuccessfulRegistration() throws Exception {
            RegisterResponse response = new RegisterResponse(1L, "newuser", "Success");
            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("newuser", "password123"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("newuser"));
        }

        @Test
        @DisplayName("Should return 400 for blank username")
        void shouldReturn400WhenUsernameBlank() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("", "password123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for short password")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("newuser", "12345"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 for existing username")
        void shouldReturn409WhenUsernameExists() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("Username already exists"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("existinguser", "password123"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== LOGIN ==========

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should return 200 on successful login")
        void shouldReturn200OnSuccessfulLogin() throws Exception {
            LoginResponse response = new LoginResponse("access-token", "refresh-token", 15L, 10080L);
            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("Should return 401 for wrong password")
        void shouldReturn401OnBadCredentials() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpass"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== REFRESH ==========

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("Should return 200 on successful token refresh")
        void shouldReturn200OnSuccessfulRefresh() throws Exception {
            LoginResponse response = new LoginResponse("new-access", "new-refresh", 15L, 10080L);
            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest("valid-refresh-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
        }

        @Test
        @DisplayName("Should return 400 for blank refresh token")
        void shouldReturn400WhenRefreshTokenBlank() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(""))))
                    .andExpect(status().isBadRequest());
        }
    }
}
