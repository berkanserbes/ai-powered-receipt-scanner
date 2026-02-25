package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.dto.response.ReceiptExtractionResult;

public interface AIService {

    ReceiptExtractionResult extractReceiptData(byte[] imageBytes);
}
