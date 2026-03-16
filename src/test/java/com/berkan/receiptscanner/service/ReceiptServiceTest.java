package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.dto.response.*;
import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.exception.ResourceNotFoundException;
import com.berkan.receiptscanner.mapper.ReceiptMapper;
import com.berkan.receiptscanner.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Tests")
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private AIService aiService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ReceiptMapper receiptMapper;

    @InjectMocks
    private ReceiptService receiptService;

    private User normalUser;
    private User adminUser;
    private User otherUser;
    private Receipt testReceipt;
    private ReceiptResponse testReceiptResponse;

    @BeforeEach
    void setUp() {
        normalUser = new User("testuser", "encoded_password", Role.USER);
        normalUser.setId(1L);

        adminUser = new User("admin", "encoded_password", Role.ADMIN);
        adminUser.setId(2L);

        otherUser = new User("otheruser", "encoded_password", Role.USER);
        otherUser.setId(3L);

        testReceipt = new Receipt(normalUser, "BIM", Instant.now(), new BigDecimal("45.50"), "TRY", "image.jpg");
        testReceipt.setId(1L);

        testReceiptResponse = new ReceiptResponse(
                1L, 1L, "testuser", "BIM", Instant.now(),
                new BigDecimal("45.50"), "TRY", "image.jpg",
                List.of(), Instant.now(), null
        );
    }

    // ========== ANALYZE ==========

    @Nested
    @DisplayName("analyzeReceipt()")
    class AnalyzeReceipt {

        @Test
        @DisplayName("Should analyze and save receipt to database")
        void shouldAnalyzeAndSaveReceipt() throws Exception {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getBytes()).thenReturn(new byte[]{1, 2, 3});

            when(fileStorageService.storeFile(mockFile)).thenReturn("stored-image.jpg");

            ReceiptExtractionResult extractionResult = new ReceiptExtractionResult(
                    "BIM", Instant.now(), new BigDecimal("45.50"), "TRY",
                    List.of(new ReceiptItemData("Ekmek", 1, new BigDecimal("5.00"), new BigDecimal("5.00")))
            );
            when(aiService.extractReceiptData(any(byte[].class))).thenReturn(extractionResult);
            when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
                Receipt receipt = invocation.getArgument(0);
                receipt.setId(1L);
                return receipt;
            });
            when(receiptMapper.toResponse(any(Receipt.class))).thenReturn(testReceiptResponse);

            ReceiptResponse result = receiptService.analyzeReceipt(mockFile, normalUser);

                assertAll("Receipt analysis assertions",
                    () -> assertNotNull(result),
                    () -> assertEquals("BIM", result.merchantName()),
                    () -> assertEquals(new BigDecimal("45.50"), result.totalAmount())
            );

            verify(fileStorageService).storeFile(mockFile);
            verify(aiService).extractReceiptData(any(byte[].class));
            verify(receiptRepository).save(any(Receipt.class));
        }
    }

    // ========== GET ALL ==========

    @Nested
    @DisplayName("getAllReceipts()")
    class GetAllReceipts {

        @Test
        @DisplayName("Admin should see all receipts")
        void adminShouldSeeAllReceipts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Receipt> page = new PageImpl<>(List.of(testReceipt));

            when(receiptRepository.findAll(pageable)).thenReturn(page);
            when(receiptMapper.toResponse(testReceipt)).thenReturn(testReceiptResponse);

            Page<ReceiptResponse> result = receiptService.getAllReceipts(pageable, adminUser);

            assertEquals(1, result.getTotalElements());
            verify(receiptRepository).findAll(pageable);
            verify(receiptRepository, never()).findByUser(any(), any());
        }

        @Test
        @DisplayName("Regular user should see only own receipts")
        void userShouldSeeOnlyOwnReceipts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Receipt> page = new PageImpl<>(List.of(testReceipt));

            when(receiptRepository.findByUser(normalUser, pageable)).thenReturn(page);
            when(receiptMapper.toResponse(testReceipt)).thenReturn(testReceiptResponse);

            Page<ReceiptResponse> result = receiptService.getAllReceipts(pageable, normalUser);

            assertEquals(1, result.getTotalElements());
            verify(receiptRepository).findByUser(normalUser, pageable);
            verify(receiptRepository, never()).findAll(any(Pageable.class));
        }
    }

    // ========== GET BY ID ==========

    @Nested
    @DisplayName("getReceiptById()")
    class GetReceiptById {

        @Test
        @DisplayName("Owner should see own receipt")
        void ownerShouldSeeOwnReceipt() {
            when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
            when(receiptMapper.toResponse(testReceipt)).thenReturn(testReceiptResponse);

            ReceiptResponse result = receiptService.getReceiptById(1L, normalUser);

            assertNotNull(result);
            assertEquals(1L, result.id());
        }

        @Test
        @DisplayName("Admin should see another user's receipt")
        void adminShouldSeeAnyReceipt() {
            when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
            when(receiptMapper.toResponse(testReceipt)).thenReturn(testReceiptResponse);

            ReceiptResponse result = receiptService.getReceiptById(1L, adminUser);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should deny access to another user's receipt")
        void otherUserShouldNotSeeReceipt() {
            when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));

            assertThrows(ResourceNotFoundException.class,
                    () -> receiptService.getReceiptById(1L, otherUser));
        }

        @Test
        @DisplayName("Should throw for a non-existing receipt")
        void shouldThrowForNonExistentReceipt() {
            when(receiptRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> receiptService.getReceiptById(999L, normalUser));
        }
    }

    // ========== DOWNLOAD ==========

    @Nested
    @DisplayName("downloadReceiptFile()")
    class DownloadReceiptFile {

        @Test
        @DisplayName("Should download receipt file successfully")
        void shouldDownloadReceiptFile() {
            byte[] fileContent = new byte[]{1, 2, 3, 4, 5};
            StoredFileData storedFileData = new StoredFileData("image.jpg", fileContent, "image/jpeg");

            when(receiptRepository.findById(1L)).thenReturn(Optional.of(testReceipt));
            when(fileStorageService.loadFile("image.jpg")).thenReturn(storedFileData);

            ReceiptFileResponse result = receiptService.downloadReceiptFile(1L, normalUser);

                assertAll("File download assertions",
                    () -> assertEquals("image.jpg", result.fileName()),
                    () -> assertArrayEquals(fileContent, result.content()),
                    () -> assertEquals("image/jpeg", result.contentType())
            );
        }
    }
}
