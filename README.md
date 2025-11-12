# REST API Testing Framework

A Java-based REST API testing framework using REST-assured, JUnit 5, Lombok, and Java 21. Designed for testing SpringBoot REST APIs with JWT authentication via PING Federate.

## Features

- **Java 21** - Latest LTS Java version
- **REST-assured 5.4.0** - Powerful REST API testing library
- **JUnit 5** - Modern testing framework
- **Lombok** - Reduces boilerplate code
- **JWT Authentication** - Automatic token retrieval from PING Federate
- **Configurable** - Properties-based configuration
- **Token Caching** - Efficient token management with caching

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Access to PING Federate server
- Client ID and Secret for PING Federate authentication

## Project Structure

```
rest-api-testing/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/insurance/api/testing/
│   │   │       ├── auth/
│   │   │       │   └── AuthenticationService.java
│   │   │       └── config/
│   │   │           └── TestConfig.java
│   |   |       └── BaseApiTest.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/insurance/api/testing/
│               └── auth/
│               |   └── AuthenticationServiceTest.java
│               |   └── BypassTokenCacheTest.java
│               |   └── OAuthScopesTest.java
│               └── BaseApiTestAnnotationTests.java
├── pom.xml
└── README.md
```

## Configuration

### 1. Environment Variables

Set the following environment variables:

```bash
export PING_CLIENT_ID=your-client-id
export PING_CLIENT_SECRET=your-client-secret
```

### 2. Application Properties

Edit `src/main/resources/application.properties`:

```properties
# PING Federate Configuration
ping.federate.base.url=https://your-ping-federate-server.com
ping.federate.token.endpoint=/as/token.oauth2
ping.federate.client.id=${PING_CLIENT_ID}
ping.federate.client.secret=${PING_CLIENT_SECRET}
ping.federate.grant.type=client_credentials

# API Base URL
api.base.url=https://your-api-server.com/api

# Test Configuration
test.timeout=30000
test.connection.timeout=10000
```

**Note:** Environment variables take precedence over properties file values.

## Building the Project

```bash
mvn clean compile
```

## Running Tests

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=ExampleApiTest
```

### Run with example tests enabled:
```bash
mvn test -Drun.example.tests=true
```

### Run with system properties:
```bash
mvn test -DPING_CLIENT_ID=your-id -DPING_CLIENT_SECRET=your-secret
```

## Writing Tests

### Creating a New Test Class

1. Extend `BaseApiTest`:
```java
public class MyApiTest extends BaseApiTest {
    // Your tests here
}
```

2. Use `getAuthenticatedRequest()` to get a pre-authenticated request:
```java
@Test
public void testMyEndpoint() {
    Response response = getAuthenticatedRequest()
        .when()
        .get("/my/endpoint")
        .then()
        .spec(responseSpec)
        .statusCode(200)
        .extract()
        .response();
}
```

### Authentication Flow

The framework automatically:
1. Retrieves JWT token from PING Federate using client credentials
2. Caches the token for 55 minutes (to avoid excessive token requests)
3. Adds the token to all requests via `Authorization: Bearer <token>` header

### Customizing Request/Response Specifications

Override methods in your test class:

```java
@Override
protected void customizeRequestSpec(RequestSpecBuilder requestSpecBuilder) {
    requestSpecBuilder.addHeader("X-Custom-Header", "value");
}

@Override
protected void customizeResponseSpec(ResponseSpecBuilder responseSpecBuilder) {
    responseSpecBuilder.expectStatusCode(200);
}
```

## Example Test

See `ExampleApiTest.java` for complete examples including:
- GET endpoint testing
- POST endpoint testing
- Unauthorized access testing
- Response validation

## Dependencies

- **REST-assured 5.4.0** - REST API testing
- **JUnit 5.10.1** - Testing framework
- **Lombok 1.18.30** - Code generation
- **Jackson 2.16.1** - JSON processing
- **SLF4J 2.0.9** - Logging

## Troubleshooting

### Token Retrieval Fails

1. Verify PING Federate URL and endpoint are correct
2. Check client ID and secret are valid
3. Ensure network connectivity to PING Federate server
4. Review logs for detailed error messages

### Tests Fail with 401/403

1. Verify token is being retrieved successfully (check logs)
2. Ensure token format is correct (should start with "Bearer ")
3. Check token hasn't expired
4. Verify API endpoint accepts the token format

### Configuration Not Loading

1. Ensure `application.properties` is in `src/main/resources`
2. Check property names match exactly (case-sensitive)
3. Use environment variables as alternative

## Best Practices

1. **Token Management**: The framework caches tokens automatically. Use `authService.invalidateToken()` if you need to force token refresh.

2. **Test Isolation**: Each test gets a fresh request specification with authentication.

3. **Error Handling**: Always validate response status codes and check response bodies for error details.

4. **Logging**: Use the provided SLF4J logging for debugging test execution.

5. **Configuration**: Prefer environment variables for sensitive data (client secrets) over properties files.

## License

This is a proprietary testing framework for internal use.

## Support

## pom.xml needs
```
<dependency>
    <groupId>com.wfld.testing</groupId>
    <artifactId>rest-api-testing</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>
```
## Example Usage for Java
```
package com.wfld.testing.api;

import com.wfld.testing.api.auth.OAuthScopes;
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
@OAuthScopes({"geocode:location:read"})
public class SysGeocodeTests extends BaseApiTest {

    private final String endpoint = "/dom-geocode-v1/api/location";

    @Test
    @DisplayName("Test unauthorized access without token")
    public void testUnauthorizedAccess() {
        log.info("Testing unauthorized access");

        Response response = given()
            .baseUri(getConfig().getApiBaseUrl())
            .contentType(io.restassured.http.ContentType.JSON)
            .when()
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
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
    @OAuthScopes({""})
    public void testAuthorizationFailsWithIncorrectScopes() {
        log.info("Testing authorization fails with incorrect OAuth scopes");

        getAuthenticatedRequest()
            .when()
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
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
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
            .then()
            .spec(responseSpec)
            .statusCode(200)
            .body("inCity", notNullValue())
            .body("inState", not(emptyString()))
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
            .get("/dom-geocode-v1/actuator/health")
            .then()
            .spec(responseSpec)
            .statusCode(200)
            .body("status", notNullValue());
    }
    
}
```
