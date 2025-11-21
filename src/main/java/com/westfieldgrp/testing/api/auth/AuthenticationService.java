package com.westfieldgrp.testing.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.westfieldgrp.testing.api.config.TestConfig;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for authenticating with PING Federate and retrieving JWT tokens.
 * Supports token caching per scope combination to avoid unnecessary token requests.
 */
@Slf4j
public class AuthenticationService {
    private static AuthenticationService instance;
    private final TestConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Playwright playwright;
    private APIRequestContext apiRequestContext;
    
    // Token cache: key is scope combination (sorted), value is TokenCacheEntry
    private final Map<String, TokenCacheEntry> tokenCache = new ConcurrentHashMap<>();
    
    // Cache entry to store token and expiry time
    private static class TokenCacheEntry {
        final String token;
        final long expiryTime;
        
        TokenCacheEntry(String token, long expiryTime) {
            this.token = token;
            this.expiryTime = expiryTime;
        }
        
        boolean isValid() {
            return System.currentTimeMillis() < expiryTime;
        }
    }

    private AuthenticationService() {
        this.config = TestConfig.getInstance();
        initializePlaywright();
    }

    private void initializePlaywright() {
        if (playwright == null) {
            this.playwright = Playwright.create();
            // Create a basic API request context for token requests
            this.apiRequestContext = playwright.request().newContext(
                new com.microsoft.playwright.APIRequest.NewContextOptions()
                    .setIgnoreHTTPSErrors(true) // Use only in test environments
            );
        }
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
        return getAccessToken(new String[0], false);
    }

    /**
     * Retrieves a JWT token from PING Federate using client credentials with specified scopes
     *
     * @param scopes OAuth scopes to include in the token request
     * @return JWT access token
     */
    public String getAccessToken(String[] scopes) {
        return getAccessToken(scopes, false);
    }

    /**
     * Retrieves a JWT token from PING Federate using client credentials with specified scopes
     *
     * @param scopes OAuth scopes to include in the token request
     * @param bypassCache if true, bypasses the token cache and fetches a new token
     * @return JWT access token
     */
    public String getAccessToken(String[] scopes, boolean bypassCache) {
        String scopeKey = createScopeKey(scopes);
        
        // Check cache if not bypassing
        if (!bypassCache) {
            TokenCacheEntry cachedEntry = tokenCache.get(scopeKey);
            if (cachedEntry != null && cachedEntry.isValid()) {
                log.debug("Using cached JWT token for scopes: {}", 
                    scopes != null && scopes.length > 0 ? String.join(", ", scopes) : "none");
                return cachedEntry.token;
            } else if (cachedEntry != null) {
                log.debug("Cached token expired for scopes: {}, fetching new token", 
                    scopes != null && scopes.length > 0 ? String.join(", ", scopes) : "none");
                tokenCache.remove(scopeKey);
            }
        } else {
            log.debug("Bypassing token cache as requested");
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

        String tokenUrl = baseUrl + tokenEndpoint;
        
        APIResponse response = apiRequestContext.post(tokenUrl,
            RequestOptions.create()
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .setData(buildFormData(formParams)));

        if (response.status() != 200) {
            log.error("Failed to retrieve token. Status: {}, Body: {}", 
                response.status(), response.text());
            throw new RuntimeException(
                "Failed to retrieve JWT token from PING Federate. Status: " + response.status()
            );
        }

        // Parse JSON response
        JsonNode jsonResponse;
        try {
            jsonResponse = objectMapper.readTree(response.text());
        } catch (Exception e) {
            log.error("Failed to parse token response as JSON: {}", response.text(), e);
            throw new RuntimeException("Failed to parse PING Federate response", e);
        }

        JsonNode accessTokenNode = jsonResponse.get("access_token");
        if (accessTokenNode == null || accessTokenNode.isNull()) {
            throw new RuntimeException("Access token not found in PING Federate response");
        }

        String accessToken = accessTokenNode.asText();
        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("Access token not found in PING Federate response");
        }

        // Cache the token per scope combination (assuming 1 hour expiry, adjust based on actual token expiry)
        // In production, you should parse the JWT to get the actual expiry time
        long expiryTime = System.currentTimeMillis() + (55 * 60 * 1000); // 55 minutes cache
        tokenCache.put(scopeKey, new TokenCacheEntry(accessToken, expiryTime));
        
        log.info("Successfully retrieved and cached JWT token for scopes: {}", 
            scopes != null && scopes.length > 0 ? String.join(", ", scopes) : "none");
        return accessToken;
    }
    
    /**
     * Creates a cache key from the scope array.
     * Sorts scopes to ensure consistent keys regardless of order.
     *
     * @param scopes array of scope strings
     * @return cache key string
     */
    private String createScopeKey(String[] scopes) {
        if (scopes == null || scopes.length == 0) {
            return "";
        }
        // Sort scopes to ensure consistent cache keys
        String[] sortedScopes = Arrays.copyOf(scopes, scopes.length);
        Arrays.sort(sortedScopes);
        return String.join(" ", sortedScopes);
    }

    /**
     * Invalidates the cached token for the specified scopes, forcing a new token to be retrieved on next call
     *
     * @param scopes OAuth scopes to invalidate cache for
     */
    public void invalidateToken(String[] scopes) {
        String scopeKey = createScopeKey(scopes);
        if (tokenCache.remove(scopeKey) != null) {
            log.info("Invalidated cached JWT token for scopes: {}", 
                scopes != null && scopes.length > 0 ? String.join(", ", scopes) : "none");
        }
    }

    /**
     * Invalidates all cached tokens, forcing new tokens to be retrieved on next call
     */
    public void invalidateAllTokens() {
        log.info("Invalidating all cached JWT tokens");
        tokenCache.clear();
    }

    /**
     * Invalidates the cached token for the default (no scopes) case
     * @deprecated Use {@link #invalidateToken(String[])} instead
     */
    @Deprecated
    public void invalidateToken() {
        invalidateToken(new String[0]);
    }

    /**
     * Builds form-encoded data string from a map of parameters
     *
     * @param formParams map of form parameters
     * @return URL-encoded form data string
     */
    private String buildFormData(Map<String, String> formParams) {
        StringBuilder formData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            if (!first) {
                formData.append("&");
            }
            formData.append(entry.getKey())
                    .append("=")
                    .append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
            first = false;
        }
        return formData.toString();
    }
}

