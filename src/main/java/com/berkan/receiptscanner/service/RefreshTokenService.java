package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.entity.RefreshToken;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.exception.InvalidRequestException;
import com.berkan.receiptscanner.exception.TokenExpiredException;
import com.berkan.receiptscanner.exception.TokenRevokedException;
import com.berkan.receiptscanner.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-minutes}")
    private long refreshTokenExpirationMinutes;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Get refresh token expiration time in minutes.
     * Used by frontend to know when to login again.
     * 
     * @return expiration time in minutes
     */
    public long getRefreshTokenExpirationInMinutes() {
        return refreshTokenExpirationMinutes;
    }

    /**
     * Create a new refresh token for the user.
     * Does NOT delete existing tokens - use revokeAllUserTokens() if needed.
     * 
     * @param user user to create token for
     * @return created refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                Instant.now().plusSeconds(refreshTokenExpirationMinutes * 60));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify that refresh token is not expired and not revoked.
     * 
     * @param token refresh token to verify
     * @return the same token if valid
     * @throws TokenExpiredException if token has expired
     * @throws TokenRevokedException if token has been revoked
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new TokenRevokedException("Refresh token has been revoked");
        }
        
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException("Refresh token has expired. Please login again.");
        }
        
        return token;
    }

    /**
     * Find refresh token by token string.
     * 
     * @param token token string
     * @return refresh token entity
     * @throws InvalidRequestException if token not found
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRequestException("Invalid refresh token"));
    }

    /**
     * Revoke a single refresh token.
     * Marks the token as revoked instead of deleting it (audit trail).
     * 
     * @param token token to revoke
     */
    @Transactional
    public void revokeToken(RefreshToken token) {
        token.revoke();
        refreshTokenRepository.save(token);
    }

    /**
     * Revoke all refresh tokens for a user.
     * Called during login to invalidate old sessions.
     * 
     * @param user user whose tokens should be revoked
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}
