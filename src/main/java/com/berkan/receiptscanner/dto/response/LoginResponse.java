package com.berkan.receiptscanner.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {
    public LoginResponse(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn, refreshExpiresIn);
    }
}
