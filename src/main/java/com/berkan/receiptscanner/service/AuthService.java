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
import com.berkan.receiptscanner.exception.InvalidRequestException;
import com.berkan.receiptscanner.repository.UserRepository;
import com.berkan.receiptscanner.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user account.
     * Creates user with USER role. User must login separately to get tokens.
     * 
     * @param request registration details
     * @return registration response with user details
     * @throws DuplicateResourceException if username already exists
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.USER);
        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                "User registered successfully. Please login to continue."
        );
    }

    /**
     * Authenticate user and generate tokens.
     * Revokes all existing refresh tokens for the user before creating new ones.
     * 
     * @param request login credentials
     * @return login response with access and refresh tokens
     * @throws InvalidRequestException if user not found
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        // Revoke all existing refresh tokens for this user (security best practice)
        refreshTokenService.revokeAllUserTokens(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationInMinutes(),
                refreshTokenService.getRefreshTokenExpirationInMinutes()
        );
    }

    /**
     * Refresh access token using a valid refresh token.
     * Implements refresh token rotation: revokes old token and issues new one.
     * 
     * @param request refresh token request
     * @return login response with new access and refresh tokens
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken oldRefreshToken = refreshTokenService.findByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(oldRefreshToken);

        User user = oldRefreshToken.getUser();
        
        // Revoke the old refresh token (rotation for security)
        refreshTokenService.revokeToken(oldRefreshToken);
        
        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
                accessToken,
                newRefreshToken.getToken(),
                jwtService.getAccessTokenExpirationInMinutes(),
                refreshTokenService.getRefreshTokenExpirationInMinutes()
        );
    }
}
