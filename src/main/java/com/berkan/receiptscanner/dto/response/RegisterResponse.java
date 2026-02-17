package com.berkan.receiptscanner.dto.response;

public record RegisterResponse(
        Long id,
        String username,
        String message
) {
}
