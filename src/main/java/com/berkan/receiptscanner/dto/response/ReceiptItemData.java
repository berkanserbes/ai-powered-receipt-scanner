package com.berkan.receiptscanner.dto.response;

import java.math.BigDecimal;

public record ReceiptItemData(
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {
}
