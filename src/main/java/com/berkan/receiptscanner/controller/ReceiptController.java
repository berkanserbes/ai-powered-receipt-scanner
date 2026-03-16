package com.berkan.receiptscanner.controller;

import com.berkan.receiptscanner.dto.response.ApiResponse;
import com.berkan.receiptscanner.dto.response.ReceiptFileResponse;
import com.berkan.receiptscanner.dto.response.ReceiptResponse;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${api.base-path}/receipts")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @Operation(summary = "Analyze and save a receipt image")
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReceiptResponse>> analyzeReceipt(
            @Parameter(description = "Receipt image file (JPEG, PNG, PDF)", required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        ReceiptResponse response = receiptService.analyzeReceipt(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Receipt analyzed and saved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReceiptResponse>>> getAllReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReceiptResponse> receipts = receiptService.getAllReceipts(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(receipts, "Receipts retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ReceiptResponse response = receiptService.getReceiptById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Receipt retrieved successfully"));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<ByteArrayResource> viewReceiptById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ReceiptFileResponse response = receiptService.downloadReceiptFile(id, currentUser);
        ByteArrayResource resource = new ByteArrayResource(response.content());

        return ResponseEntity.ok()
                .contentType(resolveMediaType(response.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(response.fileName()).build().toString())
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadReceiptById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        ReceiptFileResponse response = receiptService.downloadReceiptFile(id, currentUser);
        ByteArrayResource resource = new ByteArrayResource(response.content());

        return ResponseEntity.ok()
                .contentType(resolveMediaType(response.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(response.fileName()).build().toString())
                .body(resource);
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
