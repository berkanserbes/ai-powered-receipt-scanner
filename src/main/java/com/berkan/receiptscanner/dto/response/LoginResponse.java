package com.berkan.receiptscanner.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInMinutes,
        long refreshTokenExpiresInMinutes
) {
    public LoginResponse(String accessToken, String refreshToken, long accessTokenExpiresInMinutes,
            long refreshTokenExpiresInMinutes) {
        this(accessToken, refreshToken, "Bearer", accessTokenExpiresInMinutes, refreshTokenExpiresInMinutes);
    }
}
