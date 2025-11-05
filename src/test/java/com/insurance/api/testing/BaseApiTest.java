package com.insurance.api.testing;

import com.insurance.api.testing.auth.AuthenticationService;
import com.insurance.api.testing.auth.OAuthScopes;
import com.insurance.api.testing.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static org.hamcrest.Matchers.lessThan;

/**
 * Base test class that sets up REST-assured configuration and authentication
 * All test classes should extend this class
 */
@Slf4j
@Getter
public abstract class BaseApiTest {
    protected static TestConfig config;
    protected static AuthenticationService authService;
    protected RequestSpecification requestSpec;
    protected ResponseSpecification responseSpec;

    @BeforeAll
    public static void setUpTestSuite() {
        log.info("Initializing test suite configuration");
        config = TestConfig.getInstance();
        authService = AuthenticationService.getInstance();

        // Configure REST-assured defaults
        RestAssured.baseURI = config.getApiBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        log.info("Test suite initialized. API Base URL: {}", config.getApiBaseUrl());
    }

    @BeforeEach
    public void setUpTest() {
        log.info("Setting up test: {}", this.getClass().getSimpleName());

        // Check for @OAuthScopes annotation on the test class
        String[] scopes = extractScopesFromAnnotation();
        
        // Get JWT token for authentication with scopes
        String accessToken = authService.getAccessToken(scopes);

        // Build request specification with authentication
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
            .setBaseUri(config.getApiBaseUrl())
            .setContentType(ContentType.JSON)
            .addHeader("Authorization", "Bearer " + accessToken)
            .setRelaxedHTTPSValidation(); // Use only in test environments

        // Add any additional request specifications
        customizeRequestSpec(requestSpecBuilder);

        requestSpec = requestSpecBuilder.build();

        // Build response specification
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder()
            .expectResponseTime(lessThan(Duration.ofSeconds(30).toMillis()))
            .expectContentType(ContentType.JSON);

        // Add any additional response specifications
        customizeResponseSpec(responseSpecBuilder);

        responseSpec = responseSpecBuilder.build();

        log.info("Test setup completed");
    }

    /**
     * Override this method to customize request specifications for specific test classes
     *
     * @param requestSpecBuilder the request spec builder to customize
     */
    protected void customizeRequestSpec(RequestSpecBuilder requestSpecBuilder) {
        // Default implementation - override in subclasses if needed
    }

    /**
     * Override this method to customize response specifications for specific test classes
     *
     * @param responseSpecBuilder the response spec builder to customize
     */
    protected void customizeResponseSpec(ResponseSpecBuilder responseSpecBuilder) {
        // Default implementation - override in subclasses if needed
    }

    /**
     * Helper method to get authenticated request specification
     *
     * @return RequestSpecification with authentication headers
     */
    protected RequestSpecification getAuthenticatedRequest() {
        return RestAssured.given(requestSpec);
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
     * Extracts OAuth scopes from the @OAuthScopes annotation on the test class
     *
     * @return array of scope strings, or empty array if no annotation is present
     */
    private String[] extractScopesFromAnnotation() {
        OAuthScopes annotation = this.getClass().getAnnotation(OAuthScopes.class);
        if (annotation != null) {
            String[] scopes = annotation.value();
            if (scopes != null && scopes.length > 0) {
                log.debug("Found @OAuthScopes annotation with {} scope(s) on {}", 
                    scopes.length, this.getClass().getSimpleName());
                return scopes;
            }
        }
        log.debug("No @OAuthScopes annotation found on {}, using default (no scopes)", 
            this.getClass().getSimpleName());
        return new String[0];
    }
}

