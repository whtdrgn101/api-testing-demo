package com.westfieldgrp.testing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.westfieldgrp.testing.api.auth.AuthenticationService;
import com.westfieldgrp.testing.api.auth.BypassTokenCache;
import com.westfieldgrp.testing.api.auth.OAuthScopes;
import com.westfieldgrp.testing.api.config.TestConfig;
import com.westfieldgrp.testing.api.playwright.PlaywrightApiRequest;
import com.westfieldgrp.testing.api.template.TemplateService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base test class that sets up Playwright API testing configuration and authentication.
 * All test classes should extend this class.
 */
@Slf4j
@Getter
public abstract class BaseApiTest {
    protected static TestConfig config;
    protected static AuthenticationService authService;
    protected static TemplateService templateService;
    protected static Playwright playwright;
    protected static ObjectMapper objectMapper = new ObjectMapper();
    protected APIRequestContext apiRequestContext;

    @BeforeAll
    public static void setUpTestSuite() {
        log.info("Initializing test suite configuration");
        config = TestConfig.getInstance();
        authService = AuthenticationService.getInstance();
        templateService = TemplateService.getInstance();
        playwright = Playwright.create();

        log.info("Test suite initialized. API Base URL: {}", config.getApiBaseUrl());
    }

    @BeforeEach
    public void setUpTest(TestInfo testInfo) {
        log.info("Setting up test: {}", this.getClass().getSimpleName());

        // Check for @OAuthScopes annotation on the test method or class
        String[] scopes = extractScopesFromAnnotation(testInfo);
        
        // Check for @BypassTokenCache annotation on the test method or class
        boolean bypassCache = shouldBypassCache(testInfo);
        
        // Get JWT token for authentication with scopes (with optional cache bypass)
        String accessToken = authService.getAccessToken(scopes, bypassCache);

        // Create API request context with base configuration
        APIRequest.NewContextOptions contextOptions = new APIRequest.NewContextOptions()
            .setBaseURL(config.getApiBaseUrl())
            .setExtraHTTPHeaders(Map.of(
                "Authorization", "Bearer " + accessToken,
                "Content-Type", "application/json"
            ))
            .setTimeout(config.getTestTimeout())
            .setIgnoreHTTPSErrors(true); // Use only in test environments

        // Allow customization
        customizeApiRequestContext(contextOptions);

        apiRequestContext = playwright.request().newContext(contextOptions);

        log.info("Test setup completed");
    }

    @AfterEach
    public void tearDownTest() {
        if (apiRequestContext != null) {
            apiRequestContext.dispose();
        }
    }

    @AfterAll
    public static void tearDownTestSuite() {
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Override this method to customize API request context for specific test classes
     *
     * @param options the API request context options to customize
     */
    protected void customizeApiRequestContext(APIRequest.NewContextOptions options) {
        // Default implementation - override in subclasses if needed
    }

    /**
     * Helper method to get authenticated API request context
     *
     * @return APIRequestContext with authentication headers
     */
    protected APIRequestContext getApiRequestContext() {
        return apiRequestContext;
    }

    /**
     * Helper method to get a fluent API request builder (REST-assured-like syntax)
     *
     * @return PlaywrightApiRequest builder for fluent API calls
     */
    protected PlaywrightApiRequest request() {
        return new PlaywrightApiRequest(apiRequestContext);
    }

    /**
     * Helper method to make a GET request
     */
    protected APIResponse get(String path) {
        return apiRequestContext.get(path);
    }

    /**
     * Helper method to make a POST request
     */
    protected APIResponse post(String path, String body) {
        return apiRequestContext.post(path, 
            RequestOptions.create().setData(body));
    }

    /**
     * Helper method to make a PUT request
     */
    protected APIResponse put(String path, String body) {
        return apiRequestContext.put(path, 
            RequestOptions.create().setData(body));
    }

    /**
     * Helper method to make a DELETE request
     */
    protected APIResponse delete(String path) {
        return apiRequestContext.delete(path);
    }

    /**
     * Helper method to assert response status
     */
    protected void assertStatus(APIResponse response, int expectedStatus) {
        assertEquals(expectedStatus, response.status(), 
            String.format("Expected status %d but got %d. Response: %s", 
                expectedStatus, response.status(), response.text()));
    }

    /**
     * Helper method to parse JSON response
     */
    protected JsonNode parseJsonResponse(APIResponse response) {
        try {
            return objectMapper.readTree(response.text());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response: " + response.text(), e);
        }
    }

    /**
     * Helper method to assert JSON field value
     */
    protected void assertJsonField(APIResponse response, String fieldPath, String expectedValue) {
        JsonNode json = parseJsonResponse(response);
        String actualValue = json.at(fieldPath).asText();
        assertEquals(expectedValue, actualValue, 
            String.format("Field %s: expected '%s' but got '%s'", 
                fieldPath, expectedValue, actualValue));
    }

    /**
     * Helper method to get JSON field value
     */
    protected String getJsonField(APIResponse response, String fieldPath) {
        JsonNode json = parseJsonResponse(response);
        return json.at(fieldPath).asText();
    }

    /**
     * Get the test configuration instance
     *
     * @return TestConfig instance
     */
    protected static TestConfig getConfig() {
        return config;
    }

    /**
     * Renders a Velocity template with the provided context variables.
     * Templates are loaded from the classpath (typically src/main/resources or src/test/resources).
     * This is a convenience method that uses the shared TemplateService instance.
     *
     * @param templatePath path to the template file (e.g., "templates/user-create.json.vm")
     * @param context      map of variables to use in the template
     * @return rendered template as a string (typically JSON)
     */
    protected String renderTemplate(String templatePath, Map<String, Object> context) {
        return templateService.render(templatePath, context);
    }

    /**
     * Renders a Velocity template with the provided context variables.
     * Convenience method that accepts varargs for simple key-value pairs.
     *
     * @param templatePath path to the template file
     * @param context      varargs of key-value pairs (must be even number of arguments)
     * @return rendered template as a string (typically JSON)
     */
    protected String renderTemplate(String templatePath, Object... context) {
        return templateService.render(templatePath, context);
    }

    /**
     * Renders a Velocity template using data loaded from a JSON file.
     * The JSON file is loaded from the classpath, parsed, and used as context for the main template.
     * 
     * This is useful when you want to:
     * - Store test data in JSON files with arrays of values
     * - Reuse test data across multiple templates
     * - Separate test data from template structure
     *
     * @param templatePath     path to the main template file (e.g., "templates/user-create.json.vm")
     * @param dataFilePath     path to the JSON data file (e.g., "templates/user-data.json")
     * @param additionalContext optional map of additional variables to merge with the JSON data (takes precedence)
     * @return rendered main template as a string (typically JSON)
     */
    protected String renderTemplate(String templatePath, String dataFilePath, Map<String, Object> additionalContext) {
        return templateService.render(templatePath, dataFilePath, additionalContext);
    }

    /**
     * Renders a Velocity template using data loaded from a JSON file.
     * Convenience method that accepts varargs for additional context variables.
     *
     * @param templatePath     path to the main template file
     * @param dataFilePath     path to the JSON data file
     * @param additionalContext varargs of key-value pairs to merge with JSON data (must be even number of arguments)
     * @return rendered main template as a string (typically JSON)
     */
    protected String renderTemplate(String templatePath, String dataFilePath, Object... additionalContext) {
        return templateService.render(templatePath, dataFilePath, additionalContext);
    }

    /**
     * Loads a CSV file from the classpath and parses it into a Map for use as template context.
     * The first row is treated as headers (column names), and the specified data row
     * is parsed into a Map with header names as keys.
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @param rowIndex    zero-based index of the data row to parse (0 = first data row after header)
     * @return Map with column headers as keys and row values as values
     */
    protected Map<String, String> loadCsvAsMap(String csvFilePath, int rowIndex) {
        return templateService.loadCsvAsMap(csvFilePath, rowIndex);
    }

    /**
     * Loads a CSV file from the classpath and parses the first data row into a Map.
     * Convenience method that uses the first data row (rowIndex = 0).
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @return Map with column headers as keys and first row values as values
     */
    protected Map<String, String> loadCsvAsMap(String csvFilePath) {
        return templateService.loadCsvAsMap(csvFilePath);
    }

    /**
     * Loads a CSV file from the classpath and parses all data rows into a List of Maps.
     * Useful for iterating over multiple test data rows.
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @return List of Maps, where each Map represents a data row with headers as keys
     */
    protected List<Map<String, String>> loadCsvAsList(String csvFilePath) {
        return templateService.loadCsvAsList(csvFilePath);
    }

    /**
     * Get the TemplateService instance for advanced template operations.
     *
     * @return TemplateService instance
     */
    protected static TemplateService getTemplateService() {
        return templateService;
    }

    /**
     * Extracts OAuth scopes from the @OAuthScopes annotation.
     * First checks the test method annotation, then falls back to class-level annotation.
     * Method-level annotations take precedence over class-level annotations.
     *
     * @param testInfo JUnit TestInfo containing information about the current test
     * @return array of scope strings, or empty array if no annotation is present
     */
    private String[] extractScopesFromAnnotation(TestInfo testInfo) {
        // First, try to find the current test method using TestInfo
        Method testMethod = findTestMethod(testInfo);
        
        // Check method-level annotation first (takes precedence)
        if (testMethod != null) {
            OAuthScopes methodAnnotation = testMethod.getAnnotation(OAuthScopes.class);
            if (methodAnnotation != null) {
                String[] scopes = methodAnnotation.value();
                if (scopes != null && scopes.length > 0) {
                    log.debug("Found @OAuthScopes annotation with {} scope(s) on method {}", 
                        scopes.length, testMethod.getName());
                    return scopes;
                }
            }
        }
        
        // Fall back to class-level annotation
        OAuthScopes classAnnotation = this.getClass().getAnnotation(OAuthScopes.class);
        if (classAnnotation != null) {
            String[] scopes = classAnnotation.value();
            if (scopes != null && scopes.length > 0) {
                log.debug("Found @OAuthScopes annotation with {} scope(s) on class {}", 
                    scopes.length, this.getClass().getSimpleName());
                return scopes;
            }
        }
        
        log.debug("No @OAuthScopes annotation found, using default (no scopes)");
        return new String[0];
    }

    /**
     * Finds the current test method using JUnit's TestInfo.
     *
     * @param testInfo JUnit TestInfo containing test method information
     * @return the test method, or null if not found
     */
    private Method findTestMethod(TestInfo testInfo) {
        if (testInfo == null || testInfo.getTestMethod().isEmpty()) {
            return null;
        }
        
        try {
            return testInfo.getTestMethod().get();
        } catch (Exception e) {
            log.debug("Could not retrieve test method from TestInfo: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if token cache should be bypassed based on @BypassTokenCache annotation.
     * Method-level annotations take precedence over class-level annotations.
     *
     * @param testInfo JUnit TestInfo containing information about the current test
     * @return true if cache should be bypassed, false otherwise
     */
    private boolean shouldBypassCache(TestInfo testInfo) {
        // First, try to find the current test method using TestInfo
        Method testMethod = findTestMethod(testInfo);
        
        // Check method-level annotation first (takes precedence)
        if (testMethod != null) {
            BypassTokenCache methodAnnotation = testMethod.getAnnotation(BypassTokenCache.class);
            if (methodAnnotation != null) {
                log.debug("Found @BypassTokenCache annotation on method {}", testMethod.getName());
                return true;
            }
        }
        
        // Fall back to class-level annotation
        BypassTokenCache classAnnotation = this.getClass().getAnnotation(BypassTokenCache.class);
        if (classAnnotation != null) {
            log.debug("Found @BypassTokenCache annotation on class {}", this.getClass().getSimpleName());
            return true;
        }
        
        return false;
    }
}
