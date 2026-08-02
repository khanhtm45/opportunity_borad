package com.opportunityboard.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenRouterClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extract_content_string() throws Exception {
        var node = mapper.readTree("\"{\\\"verdict\\\":\\\"APPROVE\\\"}\"");
        assertTrue(OpenRouterClient.extractMessageContent(node).contains("APPROVE"));
    }

    @Test
    void extract_content_array_parts() throws Exception {
        var node = mapper.readTree("[{\"type\":\"text\",\"text\":\"{\\\"verdict\\\":\\\"REVIEW\\\"}\"}]");
        assertEquals("{\"verdict\":\"REVIEW\"}", OpenRouterClient.extractMessageContent(node));
    }
}
