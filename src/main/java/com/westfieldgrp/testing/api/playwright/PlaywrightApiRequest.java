package com.westfieldgrp.testing.api.playwright;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PlaywrightApiRequest {
    private final APIRequestContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String method;
    private String url;
    private Object body;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    private APIResponse response;
    private JsonNode jsonResponse;

    public PlaywrightApiRequest(APIRequestContext context) {
        this.context = context;
    }

    // HTTP Method Builders
    public PlaywrightApiRequest get(String url) {
        this.method = "GET";
        this.url = url;
        return this;
    }

    public PlaywrightApiRequest post(String url) {
        this.method = "POST";
        this.url = url;
        return this;
    }

    public PlaywrightApiRequest put(String url) {
        this.method = "PUT";
        this.url = url;
        return this;
    }

    public PlaywrightApiRequest delete(String url) {
        this.method = "DELETE";
        this.url = url;
        return this;
    }

    public PlaywrightApiRequest patch(String url) {
        this.method = "PATCH";
        this.url = url;
        return this;
    }

    // Request Configuration
    public PlaywrightApiRequest body(String jsonBody) {
        this.body = jsonBody;
        return this;
    }

    public PlaywrightApiRequest body(Object object) {
        try {
            this.body = objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize body to JSON", e);
        }
        return this;
    }

    public PlaywrightApiRequest header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public PlaywrightApiRequest headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public PlaywrightApiRequest queryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }

    public PlaywrightApiRequest queryParams(Map<String, String> params) {
        this.queryParams.putAll(params);
        return this;
    }

    // Execute the request
    private APIResponse execute() {
        if (method == null || url == null) {
            throw new IllegalStateException("HTTP method and URL must be set before execution");
        }

        RequestOptions options = RequestOptions.create();
        
        if (body != null) {
            options.setData(body.toString());
        }
        
        // Set headers individually (Playwright doesn't have setHeaders method)
        if (!headers.isEmpty()) {
            headers.forEach(options::setHeader);
        }
        
        if (!queryParams.isEmpty()) {
            // Build query string
            StringBuilder queryString = new StringBuilder();
            queryParams.forEach((key, value) -> {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(key).append("=").append(value);
            });
            url = url + "?" + queryString.toString();
        }

        log.debug("Executing {} request to {}", method, url);

        switch (method) {
            case "GET":
                response = context.get(url, options);
                break;
            case "POST":
                response = context.post(url, options);
                break;
            case "PUT":
                response = context.put(url, options);
                break;
            case "DELETE":
                response = context.delete(url, options);
                break;
            case "PATCH":
                response = context.patch(url, options);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // Parse JSON response if content type is JSON
        String contentType = response.headers().get("content-type");
        if (contentType != null && contentType.contains("application/json") && response.text() != null && !response.text().isEmpty()) {
            try {
                jsonResponse = objectMapper.readTree(response.text());
            } catch (Exception e) {
                log.warn("Failed to parse JSON response: {}", e.getMessage());
            }
        }

        return response;
    }

    // Response validation builder
    public ResponseValidator then() {
        if (response == null) {
            execute();
        }
        return new ResponseValidator(this);
    }

    // Response extraction
    public ResponseExtractor extract() {
        if (response == null) {
            execute();
        }
        return new ResponseExtractor(this);
    }

    // Getter methods for validators
    APIResponse getResponse() {
        if (response == null) {
            execute();
        }
        return response;
    }

    JsonNode getJsonResponse() {
        if (jsonResponse == null && response != null) {
            try {
                String responseText = response.text();
                if (responseText != null && !responseText.trim().isEmpty()) {
                    jsonResponse = objectMapper.readTree(responseText);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON response", e);
            }
        }
        return jsonResponse;
    }

    /**
     * Response validation builder - provides REST-assured-like fluent API
     */
    public static class ResponseValidator {
        private final PlaywrightApiRequest request;

        ResponseValidator(PlaywrightApiRequest request) {
            this.request = request;
        }

        public ResponseValidator statusCode(int expectedStatus) {
            APIResponse response = request.getResponse();
            assertEquals(expectedStatus, response.status(),
                String.format("Expected status %d but got %d. Response: %s",
                    expectedStatus, response.status(), response.text()));
            return this;
        }

        /**
         * Validates that the response status code matches one of the expected status codes.
         * Useful for cases where multiple status codes are acceptable (e.g., 401 or 403 for unauthorized).
         *
         * @param expectedStatuses one or more expected status codes
         * @return this ResponseValidator for method chaining
         */
        public ResponseValidator statusCode(int... expectedStatuses) {
            APIResponse response = request.getResponse();
            int actualStatus = response.status();
            
            for (int expectedStatus : expectedStatuses) {
                if (actualStatus == expectedStatus) {
                    return this; // Status matches one of the expected values
                }
            }
            
            // None of the expected statuses matched
            fail(String.format("Expected status code to be one of %s but got %d. Response: %s",
                java.util.Arrays.toString(expectedStatuses), actualStatus, response.text()));
            return this; // Unreachable, but satisfies static analysis
        }

        public ResponseValidator body(String jsonPath, Object expectedValue) {
            JsonNode json = request.getJsonResponse();
            if (json == null || json.isMissingNode()) {
                fail("Response is not JSON or could not be parsed");
                return this; // Unreachable, but satisfies static analysis
            }

            // Explicit null check for static analysis
            Objects.requireNonNull(json, "JSON response must not be null");
            
            JsonNode node = json.at(jsonPath.startsWith("/") ? jsonPath : "/" + jsonPath);
            if (node == null || node.isMissingNode()) {
                fail("JSON path '" + jsonPath + "' not found in response");
                return this; // Unreachable, but satisfies static analysis
            }

            Object actualValue = getNodeValue(node);
            
            if (expectedValue instanceof String) {
                assertEquals(expectedValue, actualValue.toString(),
                    String.format("Field %s: expected '%s' but got '%s'", jsonPath, expectedValue, actualValue));
            } else if (expectedValue instanceof Number) {
                assertEquals(((Number) expectedValue).doubleValue(), ((Number) actualValue).doubleValue(),
                    String.format("Field %s: expected %s but got %s", jsonPath, expectedValue, actualValue));
            } else {
                assertEquals(expectedValue, actualValue,
                    String.format("Field %s: expected %s but got %s", jsonPath, expectedValue, actualValue));
            }

            return this;
        }

        public ResponseValidator body(String jsonPath, Consumer<JsonNode> validator) {
            JsonNode json = request.getJsonResponse();
            if (json == null || json.isMissingNode()) {
                fail("Response is not JSON or could not be parsed");
                return this; // Unreachable, but satisfies static analysis
            }

            JsonNode node = json.at(jsonPath.startsWith("/") ? jsonPath : "/" + jsonPath);
            if (node == null || node.isMissingNode()) {
                fail("JSON path '" + jsonPath + "' not found in response");
            }

            validator.accept(node);
            return this;
        }

        public ResponseValidator contentType(String expectedContentType) {
            String contentType = request.getResponse().headers().get("content-type");
            assertTrue(contentType != null && contentType.contains(expectedContentType),
                String.format("Expected content type containing '%s' but got '%s'", expectedContentType, contentType));
            return this;
        }

        public ResponseValidator header(String headerName, String expectedValue) {
            String actualValue = request.getResponse().headers().get(headerName.toLowerCase());
            assertEquals(expectedValue, actualValue,
                String.format("Header %s: expected '%s' but got '%s'", headerName, expectedValue, actualValue));
            return this;
        }

        public ResponseValidator responseTime(long maxTimeMs) {
            // Playwright doesn't expose response time directly, but we can measure it
            // This is a placeholder - you'd need to track timing yourself
            log.warn("Response time validation not directly supported by Playwright");
            return this;
        }

        private Object getNodeValue(JsonNode node) {
            if (node.isTextual()) {
                return node.asText();
            } else if (node.isNumber()) {
                if (node.isInt()) {
                    return node.asInt();
                } else if (node.isLong()) {
                    return node.asLong();
                } else {
                    return node.asDouble();
                }
            } else if (node.isBoolean()) {
                return node.asBoolean();
            } else if (node.isNull()) {
                return null;
            } else {
                return node.toString();
            }
        }
    }

    /**
     * Response extraction - provides REST-assured-like extraction methods
     */
    public static class ResponseExtractor {
        private final PlaywrightApiRequest request;

        ResponseExtractor(PlaywrightApiRequest request) {
            this.request = request;
        }

        public APIResponse response() {
            return request.getResponse();
        }

        public String asString() {
            return request.getResponse().text();
        }

        public JsonNode asJson() {
            return request.getJsonResponse();
        }

        public <T> T as(Class<T> clazz) {
            try {
                return request.objectMapper.readValue(request.getResponse().text(), clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize response to " + clazz.getName(), e);
            }
        }

        public String path(String jsonPath) {
            JsonNode json = request.getJsonResponse();
            if (json == null) {
                throw new RuntimeException("Response is not JSON or could not be parsed");
            }

            JsonNode node = json.at(jsonPath.startsWith("/") ? jsonPath : "/" + jsonPath);
            if (node == null || node.isMissingNode()) {
                throw new RuntimeException("JSON path '" + jsonPath + "' not found in response");
            }

            if (node.isTextual()) {
                return node.asText();
            } else {
                return node.toString();
            }
        }

        public <T> List<T> pathAsList(String jsonPath, Class<T> elementClass) {
            JsonNode json = request.getJsonResponse();
            if (json == null) {
                throw new RuntimeException("Response is not JSON or could not be parsed");
            }

            JsonNode node = json.at(jsonPath.startsWith("/") ? jsonPath : "/" + jsonPath);
            if (node == null || node.isMissingNode() || !node.isArray()) {
                throw new RuntimeException("JSON path '" + jsonPath + "' is not an array");
            }

            try {
                return request.objectMapper.convertValue(node, 
                    request.objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert array to List<" + elementClass.getName() + ">", e);
            }
        }
    }
}

