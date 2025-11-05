package com.insurance.api.testing.auth;

import com.insurance.api.testing.config.TestConfig;
//simport io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Service for authenticating with PING Federate and retrieving JWT tokens
 */
@Slf4j
public class AuthenticationService {
    private static AuthenticationService instance;
    private final TestConfig config;
    private String cachedToken;
    private long tokenExpiryTime;

    private AuthenticationService() {
        this.config = TestConfig.getInstance();
    }

    public static synchronized AuthenticationService getInstance() {
        if (instance == null) {
            instance = new AuthenticationService();
        }
        return instance;
    }

    /**
     * Retrieves a JWT token from PING Federate using client credentials
     *
     * @return JWT access token
     */
    public String getAccessToken() {
        return getAccessToken(new String[0]);
    }

    /**
     * Retrieves a JWT token from PING Federate using client credentials with specified scopes
     *
     * @param scopes OAuth scopes to include in the token request
     * @return JWT access token
     */
    public String getAccessToken(String[] scopes) {
        // Check if we have a valid cached token
        // Note: In a production system, you might want to cache tokens per scope combination
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            log.debug("Using cached JWT token");
            return cachedToken;
        }

        log.info("Fetching new JWT token from PING Federate with scopes: {}", 
            scopes != null && scopes.length > 0 ? String.join(", ", scopes) : "none");

        String baseUrl = config.getPingFederateBaseUrl();
        String tokenEndpoint = config.getPingFederateTokenEndpoint();
        String clientId = config.getPingFederateClientId();
        String clientSecret = config.getPingFederateClientSecret();
        String grantType = config.getPingFederateGrantType();

        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException(
                "PING Federate client ID and secret must be configured. " +
                "Set PING_CLIENT_ID and PING_CLIENT_SECRET environment variables or in application.properties"
            );
        }

        //String tokenUrl = baseUrl + tokenEndpoint;

        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", grantType);
        formParams.put("client_id", clientId);
        formParams.put("client_secret", clientSecret);
        
        // Add scopes if provided
        if (scopes != null && scopes.length > 0) {
            String scopeString = String.join(" ", scopes);
            formParams.put("scope", scopeString);
            log.debug("Including scopes in token request: {}", scopeString);
        }

        Response response = given()
            .baseUri(baseUrl)
            .contentType(ContentType.URLENC)
            .formParams(formParams)
            .when()
            .post(tokenEndpoint)
            .then()
            .extract()
            .response();

        if (response.getStatusCode() != 200) {
            log.error("Failed to retrieve token. Status: {}, Body: {}", 
                response.getStatusCode(), response.getBody().asString());
            throw new RuntimeException(
                "Failed to retrieve JWT token from PING Federate. Status: " + response.getStatusCode()
            );
        }

        String accessToken = response.jsonPath().getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("Access token not found in PING Federate response");
        }

        // Cache the token (assuming 1 hour expiry, adjust based on actual token expiry)
        // In production, you should parse the JWT to get the actual expiry time
        cachedToken = accessToken;
        tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000); // 55 minutes cache

        log.info("Successfully retrieved JWT token");
        return accessToken;
    }

    /**
     * Invalidates the cached token, forcing a new token to be retrieved on next call
     */
    public void invalidateToken() {
        log.info("Invalidating cached JWT token");
        cachedToken = null;
        tokenExpiryTime = 0;
    }
}

