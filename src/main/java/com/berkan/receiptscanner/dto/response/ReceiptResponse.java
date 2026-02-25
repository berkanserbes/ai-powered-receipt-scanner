package com.berkan.receiptscanner.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReceiptResponse(
        Long id,
        Long userId,
        String username,
        String merchantName,
        Instant transactionDate,
        BigDecimal totalAmount,
        String currency,
        String imageUrl,
        List<ReceiptItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
