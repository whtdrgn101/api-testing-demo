package com.insurance.api.testing;

import com.insurance.api.testing.auth.OAuthScopes;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Example API test class demonstrating the testing pattern
 * Replace this with your actual API endpoint tests
 * 
 * This example demonstrates the use of @OAuthScopes annotation to specify
 * OAuth scopes for token class-level and method-levelrequests. 
 * 
 * The scopes "geocode:read" and "geocode:write" will be
 * included in the token request when this test class runs
 * unless overridden by a method-level @OAuthScopes annotation.
 */
@Slf4j
@DisplayName("Geocode API Tests")
@OAuthScopes({"geocode:read", "geocode:write"})
public class ExampleApiTest extends BaseApiTest {

    private final String endpoint = "/geocode/v1/geocode";

    @Test
    @DisplayName("Test unauthorized access without token")
    public void testUnauthorizedAccess() {
        log.info("Testing unauthorized access");

        Response response = given()
            .baseUri(getConfig().getApiBaseUrl())
            .contentType(io.restassured.http.ContentType.JSON)
            .when()
            .get(this.endpoint)
            .then()
            .extract()
            .response();

        log.info("Response status: {}", response.getStatusCode());
        assertTrue(
            response.getStatusCode() == 401 || response.getStatusCode() == 403,
            "Expected 401 or 403 status code for unauthorized access"
        );
    }

    @Test
    @DisplayName("Test Authorization Failes with incorrect OAuth scopes")
    @OAuthScopes({"read", "write"})
    public void testAuthorizationFailsWithIncorrectScopes() {
        log.info("Testing authorization fails with incorrect OAuth scopes");

        getAuthenticatedRequest()
            .when()
            .get(this.endpoint)
            .then()
            .spec(responseSpec)
            .statusCode(403);
    }

    @Test
    @DisplayName("Test GET endpoint - Example")
    public void testGetEndpoint() {
        log.info("Executing GET endpoint test");

        Response response = getAuthenticatedRequest()
            .when()
            .get(this.endpoint + "?address=1600 Amphitheatre Parkway, Mountain View, CA")
            .then()
            .spec(responseSpec)
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", not(emptyString()))
            .extract()
            .response();

        log.info("Response status: {}", response.getStatusCode());
        log.info("Response body: {}", response.getBody().asString());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @DisplayName("Test healthcheck endpoint validation")
    public void testEndpointValidation() {
        log.info("Testing endpoint response validation");

        getAuthenticatedRequest()
            .when()
            .get(this.endpoint)
            .then()
            .spec(responseSpec)
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", not(emptyString()))
            .body("createdDate", not(emptyString()))
            .body("status", anyOf(is("ACTIVE"), is("INACTIVE")));
    }
    
}

