package com.berkan.receiptscanner.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReceiptExtractionResult(
        String merchant,
        Instant date,
        BigDecimal total,
        String currency,
        List<ReceiptItemData> items) {
}
