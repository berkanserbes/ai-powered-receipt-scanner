package com.berkan.receiptscanner.dto.response;

public record ReceiptFileResponse(
        String fileName,
        byte[] content,
        String contentType
) {
}
