package com.opportunityboard.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunityboard.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterClient {

    private final OpenRouterProperties props;
    private final ObjectMapper objectMapper;

    public String chat(String systemPrompt, List<Map<String, Object>> userContentParts) {
        if (!props.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình OPENROUTER_API_KEY trong backend/.env");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.model());
        body.put("temperature", 0.2);
        // Ép JSON object — khớp OpenRouter / OpenAI-compatible API
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContentParts)
        ));

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(trimSlash(props.baseUrl()))
                    .requestFactory(requestFactory())
                    .build();

            String raw = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + props.apiKey().trim())
                    .header("HTTP-Referer",
                            props.siteUrl() != null && !props.siteUrl().isBlank()
                                    ? props.siteUrl()
                                    : "https://github.com/khanhtm45/opportunity_borad")
                    .header("X-Title",
                            props.siteName() != null && !props.siteName().isBlank()
                                    ? props.siteName()
                                    : "Opportunity Board")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            if (root.has("error")) {
                String err = root.path("error").path("message").asText(root.path("error").toString());
                throw new BadRequestException("OpenRouter lỗi: " + err);
            }
            String content = extractMessageContent(root.path("choices").path(0).path("message").path("content"));
            if (content == null || content.isBlank()) {
                throw new BadRequestException("OpenRouter không trả nội dung (choices[0].message.content rỗng)");
            }
            return content;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            log.error("OpenRouter HTTP {}: {}", ex.getStatusCode().value(), detail);
            throw new BadRequestException("Gọi OpenRouter thất bại (" + ex.getStatusCode().value() + "): "
                    + (detail != null && !detail.isBlank() ? detail : ex.getMessage()));
        } catch (Exception ex) {
            log.error("OpenRouter call failed", ex);
            throw new BadRequestException("Gọi OpenRouter thất bại: " + ex.getMessage());
        }
    }

    /** Helper: text + optional image/PDF URLs (Gemini multimodal qua OpenRouter). */
    public static List<Map<String, Object>> textAndImages(String text, List<String> mediaUrls) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", text));
        if (mediaUrls != null) {
            for (String url : mediaUrls) {
                if (url == null || url.isBlank()) continue;
                String u = url.trim();
                // Gemini/OpenRouter chỉ chấp nhận http(s) — bỏ filename kiểu cv.pdf
                String lower = u.toLowerCase();
                if (!(lower.startsWith("http://") || lower.startsWith("https://"))) continue;
                parts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", u)
                ));
            }
        }
        return parts;
    }

    /** content có thể là string hoặc mảng parts (một số provider Gemini). */
    static String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) return null;
        if (contentNode.isTextual()) return contentNode.asText();
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.isTextual()) sb.append(part.asText());
                else if (part.has("text")) sb.append(part.path("text").asText(""));
            }
            return sb.toString();
        }
        return contentNode.asText(null);
    }

    private static String trimSlash(String base) {
        if (base == null || base.isBlank()) return "https://openrouter.ai/api/v1";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private JdkClientHttpRequestFactory requestFactory() {
        int timeout = Math.max(30, props.timeoutSeconds());
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(30, timeout)))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        return factory;
    }
}
