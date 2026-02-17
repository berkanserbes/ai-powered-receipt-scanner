package com.berkan.receiptscanner.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access token.
 * 
 * Used when the access token expires and the client needs a new one.
 * The client sends the refresh token, and the server validates it and returns a new access token.
 * 
 * Flow:
 * 1. Client's access token expires (after 15 minutes)
 * 2. Client sends refresh token to /api/v1/auth/refresh
 * 3. Server validates refresh token (not expired, not revoked)
 * 4. Server generates new access token
 * 5. Client continues using the API with new access token
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
