package com.berkan.receiptscanner.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for receipt line items.
 * All fields except 'id' can be null if AI couldn't extract the information.
 * 
 * Future enhancements:
 * - category: Item category (e.g., "Food", "Electronics")
 * - description: Additional item details
 * - taxAmount: Tax applied to this item
 */
public record ReceiptItemResponse(
        Long id,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
