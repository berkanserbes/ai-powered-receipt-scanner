package com.berkan.receiptscanner.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptResponse(
        Long id,
        Long userId,
        String username,
        String merchantName,
        LocalDate transactionDate,
        BigDecimal totalAmount,
        String currency,
        String imageUrl,
        List<ReceiptItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
