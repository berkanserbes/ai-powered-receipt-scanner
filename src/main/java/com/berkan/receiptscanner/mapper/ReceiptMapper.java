package com.berkan.receiptscanner.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.berkan.receiptscanner.dto.response.ReceiptItemResponse;
import com.berkan.receiptscanner.dto.response.ReceiptResponse;
import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.ReceiptItem;

@Component
public class ReceiptMapper {

    public ReceiptResponse toResponse(Receipt receipt) {
        List<ReceiptItemResponse> items = receipt.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new ReceiptResponse(
                receipt.getId(),
                receipt.getUser().getId(),
                receipt.getUser().getUsername(),
                receipt.getMerchantName(),
                receipt.getTransactionDate(),
                receipt.getTotalAmount(),
                receipt.getCurrency(),
                receipt.getImageUrl(),
                items,
                receipt.getCreatedAt(),
                receipt.getUpdatedAt()
        );
    }

    private ReceiptItemResponse toItemResponse(ReceiptItem item) {
        return new ReceiptItemResponse(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice());
    }
}
