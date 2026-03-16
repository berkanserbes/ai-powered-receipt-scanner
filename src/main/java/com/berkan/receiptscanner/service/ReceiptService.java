package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.dto.response.ReceiptExtractionResult;
import com.berkan.receiptscanner.dto.response.ReceiptFileResponse;
import com.berkan.receiptscanner.dto.response.ReceiptItemData;
import com.berkan.receiptscanner.dto.response.ReceiptResponse;
import com.berkan.receiptscanner.dto.response.StoredFileData;
import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.ReceiptItem;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.exception.ResourceNotFoundException;
import com.berkan.receiptscanner.mapper.ReceiptMapper;
import com.berkan.receiptscanner.repository.ReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);

    private final ReceiptRepository receiptRepository;
    private final AIService aiService;
    private final FileStorageService fileStorageService;
    private final ReceiptMapper receiptMapper;

    public ReceiptService(ReceiptRepository receiptRepository,
            AIService aiService,
            FileStorageService fileStorageService,
            ReceiptMapper receiptMapper) {
        this.receiptRepository = receiptRepository;
        this.aiService = aiService;
        this.fileStorageService = fileStorageService;
        this.receiptMapper = receiptMapper;
    }

    @Transactional
    public ReceiptResponse analyzeReceipt(MultipartFile file, User currentUser) {
        logger.info("Analyzing receipt for user: {}", currentUser.getUsername());

        // Store the file
        String storedFileName = fileStorageService.storeFile(file);

        // Extract data using AI
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException ex) {
            throw new com.berkan.receiptscanner.exception.FileStorageException("Could not read uploaded file", ex);
        }

        ReceiptExtractionResult extractionResult = aiService.extractReceiptData(imageBytes);

        // Save receipt to database
        Receipt receipt = new Receipt(
                currentUser,
                extractionResult.merchant(),
                extractionResult.date(),
                extractionResult.total(),
                extractionResult.currency(),
                storedFileName);

        // Add items if extracted
        if (extractionResult.items() != null && !extractionResult.items().isEmpty()) {
            for (ReceiptItemData itemData : extractionResult.items()) {
                ReceiptItem item = new ReceiptItem(
                        itemData.name(),
                        itemData.quantity(),
                        itemData.unitPrice(),
                        itemData.totalPrice());
                receipt.addItem(item);
            }
        }

        Receipt savedReceipt = receiptRepository.save(receipt);
        logger.info("Receipt saved with id: {} and {} items", savedReceipt.getId(), savedReceipt.getItems().size());

        return receiptMapper.toResponse(savedReceipt);
    }

    @Transactional(readOnly = true)
    public Page<ReceiptResponse> getAllReceipts(Pageable pageable, User currentUser) {
        Page<Receipt> receipts;

        if (currentUser.getRole() == Role.ADMIN) {
            receipts = receiptRepository.findAll(pageable);
        } else {
            receipts = receiptRepository.findByUser(currentUser, pageable);
        }

        return receipts.map(receiptMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(Long id, User currentUser) {
        Receipt receipt = getAuthorizedReceipt(id, currentUser);

        return receiptMapper.toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptFileResponse downloadReceiptFile(Long id, User currentUser) {
        Receipt receipt = getAuthorizedReceipt(id, currentUser);
        StoredFileData storedFileData = fileStorageService.loadFile(receipt.getImageUrl());
        return new ReceiptFileResponse(storedFileData.fileName(), storedFileData.content(), storedFileData.contentType());
    }

    private Receipt getAuthorizedReceipt(Long id, User currentUser) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));

        if (currentUser.getRole() != Role.ADMIN && !receipt.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Receipt not found with id: " + id);
        }

        return receipt;
    }
}
