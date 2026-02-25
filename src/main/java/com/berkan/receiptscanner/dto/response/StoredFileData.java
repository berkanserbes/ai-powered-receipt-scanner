package com.berkan.receiptscanner.dto.response;

public record StoredFileData(String fileName, byte[] content, String contentType) {
}
