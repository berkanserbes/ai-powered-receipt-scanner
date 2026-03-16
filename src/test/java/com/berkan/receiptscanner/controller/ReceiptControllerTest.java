package com.berkan.receiptscanner.controller;

import com.berkan.receiptscanner.dto.response.ReceiptFileResponse;
import com.berkan.receiptscanner.dto.response.ReceiptResponse;
import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.exception.ResourceNotFoundException;
import com.berkan.receiptscanner.filter.JwtAuthenticationFilter;
import com.berkan.receiptscanner.filter.RequestRateLimitFilter;
import com.berkan.receiptscanner.service.ReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReceiptController Tests")
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiptService receiptService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RequestRateLimitFilter requestRateLimitFilter;

    private final User testUser = createTestUser();

    private static User createTestUser() {
        User user = new User("testuser", "encoded_password", Role.USER);
        user.setId(1L);
        return user;
    }

    private ReceiptResponse createSampleResponse() {
        return new ReceiptResponse(
                1L, 1L, "testuser", "BIM", Instant.now(),
                new BigDecimal("45.50"), "TRY", "image.jpg",
                List.of(), Instant.now(), null
        );
    }

    // ========== ANALYZE ==========

    @Nested
    @DisplayName("POST /api/v1/receipts/analyze")
    class AnalyzeEndpoint {

        @Test
        @DisplayName("Should return 201 when receipt is uploaded")
        void shouldReturn201WhenReceiptUploaded() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(receiptService.analyzeReceipt(any(), any(User.class))).thenReturn(createSampleResponse());

            mockMvc.perform(multipart("/api/v1/receipts/analyze")
                            .file(file)
                            .with(user(testUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.merchantName").value("BIM"));
        }
    }

    // ========== GET ALL ==========

    @Nested
    @DisplayName("GET /api/v1/receipts")
    class GetAllEndpoint {

        @Test
        @DisplayName("Should return paged receipts")
        void shouldReturnPagedReceipts() throws Exception {
            Page<ReceiptResponse> page = new PageImpl<>(List.of(createSampleResponse()));
            when(receiptService.getAllReceipts(any(Pageable.class), any(User.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/receipts")
                            .param("page", "1")
                            .param("size", "10")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.page").value(1));
        }
    }

    // ========== GET BY ID ==========

    @Nested
    @DisplayName("GET /api/v1/receipts/{id}")
    class GetByIdEndpoint {

        @Test
        @DisplayName("Should return 200 for existing receipt")
        void shouldReturn200ForExistingReceipt() throws Exception {
            when(receiptService.getReceiptById(eq(1L), any(User.class))).thenReturn(createSampleResponse());

            mockMvc.perform(get("/api/v1/receipts/1")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("Should return 404 for non-existing receipt")
        void shouldReturn404ForNonExistentReceipt() throws Exception {
            when(receiptService.getReceiptById(eq(999L), any(User.class)))
                    .thenThrow(new ResourceNotFoundException("Receipt not found with id: 999"));

            mockMvc.perform(get("/api/v1/receipts/999")
                            .with(user(testUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== DOWNLOAD ==========

    @Nested
    @DisplayName("GET /api/v1/receipts/{id}/download")
    class DownloadEndpoint {

        @Test
        @DisplayName("Should download receipt file successfully")
        void shouldReturnFileContent() throws Exception {
            byte[] content = new byte[]{1, 2, 3, 4, 5};
            ReceiptFileResponse fileResponse = new ReceiptFileResponse("receipt.jpg", content, "image/jpeg");

            when(receiptService.downloadReceiptFile(eq(1L), any(User.class))).thenReturn(fileResponse);

            mockMvc.perform(get("/api/v1/receipts/1/download")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG));
        }
    }

    // ========== VIEW ==========

    @Nested
    @DisplayName("GET /api/v1/receipts/{id}/view")
    class ViewEndpoint {

        @Test
        @DisplayName("Should return receipt file for inline viewing")
        void shouldReturnFileForInlineViewing() throws Exception {
            byte[] content = new byte[]{1, 2, 3};
            ReceiptFileResponse fileResponse = new ReceiptFileResponse("receipt.jpg", content, "image/jpeg");

            when(receiptService.downloadReceiptFile(eq(1L), any(User.class))).thenReturn(fileResponse);

            mockMvc.perform(get("/api/v1/receipts/1/view")
                            .with(user(testUser)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")));
        }
    }

}
