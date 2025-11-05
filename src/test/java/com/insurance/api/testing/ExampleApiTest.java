package com.insurance.api.testing;

import com.insurance.api.testing.auth.OAuthScopes;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Example API test class demonstrating the testing pattern
 * Replace this with your actual API endpoint tests
 * 
 * This example demonstrates the use of @OAuthScopes annotation to specify
 * OAuth scopes for token requests. The scopes "read" and "write" will be
 * included in the token request when this test class runs.
 */
@Slf4j
@DisplayName("Example API Tests")
@OAuthScopes({"read", "write"})
public class ExampleApiTest extends BaseApiTest {

    @Test
    @DisplayName("Test GET endpoint - Example")
    @EnabledIfSystemProperty(named = "run.example.tests", matches = "true")
    public void testGetEndpoint() {
        log.info("Executing GET endpoint test");

        Response response = getAuthenticatedRequest()
            .when()
            .get("/example/endpoint")
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
    @DisplayName("Test POST endpoint - Example")
    @EnabledIfSystemProperty(named = "run.example.tests", matches = "true")
    public void testPostEndpoint() {
        log.info("Executing POST endpoint test");

        String requestBody = """
            {
                "name": "Test Name",
                "description": "Test Description"
            }
            """;

        Response response = getAuthenticatedRequest()
            .body(requestBody)
            .when()
            .post("/example/endpoint")
            .then()
            .spec(responseSpec)
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .response();

        log.info("Response status: {}", response.getStatusCode());
        log.info("Response body: {}", response.getBody().asString());

        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
    }

    @Test
    @DisplayName("Test unauthorized access without token")
    public void testUnauthorizedAccess() {
        log.info("Testing unauthorized access");

        Response response = given()
            .baseUri(getConfig().getApiBaseUrl())
            .contentType(io.restassured.http.ContentType.JSON)
            .when()
            .get("/example/endpoint")
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
    @DisplayName("Test endpoint validation")
    @EnabledIfSystemProperty(named = "run.example.tests", matches = "true")
    public void testEndpointValidation() {
        log.info("Testing endpoint response validation");

        getAuthenticatedRequest()
            .when()
            .get("/example/endpoint")
            .then()
            .spec(responseSpec)
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", not(emptyString()))
            .body("createdDate", not(emptyString()))
            .body("status", anyOf(is("ACTIVE"), is("INACTIVE")));
    }
}

