package com.berkan.receiptscanner.service;

import com.berkan.receiptscanner.config.GeminiConfig;
import com.berkan.receiptscanner.dto.response.ReceiptExtractionResult;
import com.berkan.receiptscanner.dto.response.ReceiptItemData;
import com.berkan.receiptscanner.exception.AIProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final GeminiConfig geminiConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are an expert receipt scanner. Analyze the provided image and extract the following information in strict JSON format:
            - \"merchant\": String (Name of the store/vendor)
            - \"date\": String (YYYY-MM-DD format)
            - \"total\": Number (Total amount paid)
            - \"currency\": String (ISO 4217 code, e.g., TRY, USD, EUR)
            - \"items\": Array of objects (optional, extract line items if legible). Each item should have:
              - \"name\": String (Item name/description)
              - \"quantity\": Number (Quantity purchased)
              - \"unitPrice\": Number (Price per unit)
              - \"totalPrice\": Number (Total price for this item)

            If a value is not found, use null. Do not include markdown formatting or code blocks in the response. Return ONLY raw JSON.
            """;

    public GeminiService(GeminiConfig geminiConfig, ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public ReceiptExtractionResult extractReceiptData(byte[] imageBytes) {
        if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isBlank()) {
            throw new AIProcessingException("Gemini API key is not configured");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", SYSTEM_PROMPT),
                                        Map.of("inline_data", Map.of(
                                                "mime_type", "image/jpeg",
                                                "data", base64Image))
                                )
                        )
                )
        );

        String url = String.format("%s/models/%s:generateContent?key=%s",
                geminiConfig.getApiUrl(), geminiConfig.getModel(), geminiConfig.getApiKey());

        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody);
        } catch (RuntimeException ex) {
            logger.error("Gemini API call failed: {}", ex.getMessage(), ex);
            throw new AIProcessingException("Failed to process receipt with Gemini: " + ex.getMessage(), ex);
        }
    }

    private ReceiptExtractionResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asString();

            content = content.replace("```json", "").replace("```", "").trim();
            JsonNode data = objectMapper.readTree(content);

            String merchant = data.has("merchant") && !data.get("merchant").isNull()
                    ? data.get("merchant").asString()
                    : null;
            Instant date = data.has("date") && !data.get("date").isNull()
                    ? LocalDate.parse(data.get("date").asString()).atStartOfDay().toInstant(ZoneOffset.UTC)
                    : null;
            BigDecimal total = data.has("total") && !data.get("total").isNull()
                    ? data.get("total").decimalValue()
                    : null;
            String currency = data.has("currency") && !data.get("currency").isNull()
                    ? data.get("currency").asString()
                    : null;

            List<ReceiptItemData> items = new java.util.ArrayList<>();
            if (data.has("items") && !data.get("items").isNull() && data.get("items").isArray()) {
                for (JsonNode itemNode : data.get("items")) {
                    String name = itemNode.has("name") && !itemNode.get("name").isNull()
                            ? itemNode.get("name").asString()
                            : null;
                    Integer quantity = itemNode.has("quantity") && !itemNode.get("quantity").isNull()
                            ? itemNode.get("quantity").asInt()
                            : null;
                    BigDecimal unitPrice = itemNode.has("unitPrice") && !itemNode.get("unitPrice").isNull()
                            ? itemNode.get("unitPrice").decimalValue()
                            : null;
                    BigDecimal totalPrice = itemNode.has("totalPrice") && !itemNode.get("totalPrice").isNull()
                            ? itemNode.get("totalPrice").decimalValue()
                            : null;

                    if (name != null) {
                        items.add(new ReceiptItemData(name, quantity, unitPrice, totalPrice));
                    }
                }
            }

            return new ReceiptExtractionResult(merchant, date, total, currency, items);
        } catch (RuntimeException ex) {
            logger.error("Failed to parse Gemini response: {}", ex.getMessage(), ex);
            throw new AIProcessingException("Failed to parse Gemini response: " + ex.getMessage(), ex);
        }
    }
}
