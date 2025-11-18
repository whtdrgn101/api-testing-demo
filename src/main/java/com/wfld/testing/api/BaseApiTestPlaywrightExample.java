package com.wfld.testing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.wfld.testing.api.playwright.PlaywrightApiRequest;
import com.wfld.testing.api.auth.AuthenticationService;
import com.wfld.testing.api.auth.BypassTokenCache;
import com.wfld.testing.api.auth.OAuthScopes;
import com.wfld.testing.api.config.TestConfig;
import com.wfld.testing.api.template.TemplateService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EXAMPLE/REFERENCE CODE - NOT COMPILED
 * 
 * Example implementation of BaseApiTest using Playwright instead of REST-assured.
 * This demonstrates how the library would look with Playwright's API testing features.
 * 
 * This file is excluded from compilation because Playwright is not a dependency.
 * 
 * To use this code:
 * 1. Add Playwright dependency to pom.xml
 * 2. Remove the excludes from maven-compiler-plugin in pom.xml
 * 
 * See PLAYWRIGHT_COMPARISON.md and PLAYWRIGHT_FLUENT_API.md for details.
 */
@Slf4j
@Getter
public abstract class BaseApiTestPlaywrightExample {
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
            .setTimeout(config.getTestTimeout());

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
     */
    protected static TestConfig getConfig() {
        return config;
    }

    // Template methods (same as REST-assured version)
    protected String renderTemplate(String templatePath, Map<String, Object> context) {
        return templateService.render(templatePath, context);
    }

    protected String renderTemplate(String templatePath, Object... context) {
        return templateService.render(templatePath, context);
    }

    protected String renderTemplate(String templatePath, String dataFilePath, Map<String, Object> additionalContext) {
        return templateService.render(templatePath, dataFilePath, additionalContext);
    }

    protected String renderTemplate(String templatePath, String dataFilePath, Object... additionalContext) {
        return templateService.render(templatePath, dataFilePath, additionalContext);
    }

    protected static TemplateService getTemplateService() {
        return templateService;
    }

    // Annotation extraction methods (same as REST-assured version)
    private String[] extractScopesFromAnnotation(TestInfo testInfo) {
        Method testMethod = findTestMethod(testInfo);
        
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

    private boolean shouldBypassCache(TestInfo testInfo) {
        Method testMethod = findTestMethod(testInfo);
        
        if (testMethod != null) {
            BypassTokenCache methodAnnotation = testMethod.getAnnotation(BypassTokenCache.class);
            if (methodAnnotation != null) {
                log.debug("Found @BypassTokenCache annotation on method {}", testMethod.getName());
                return true;
            }
        }
        
        BypassTokenCache classAnnotation = this.getClass().getAnnotation(BypassTokenCache.class);
        if (classAnnotation != null) {
            log.debug("Found @BypassTokenCache annotation on class {}", this.getClass().getSimpleName());
            return true;
        }
        
        return false;
    }
}