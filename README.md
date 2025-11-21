# REST API Testing Framework

A Java-based REST API testing framework using Playwright, JUnit 5, Lombok, and Java 21. Designed for testing SpringBoot REST APIs with JWT authentication via PING Federate.

## Features

- **Java 21** - Latest LTS Java version
- **Playwright 1.40.0** - Modern API and browser testing library
- **JUnit 5** - Modern testing framework
- **Lombok** - Reduces boilerplate code
- **JWT Authentication** - Automatic token retrieval from PING Federate
- **Configurable** - Properties-based configuration
- **Token Caching** - Efficient token management with caching
- **Velocity Templates** - Generate JSON request bodies from templates
- **CSV Data Loading** - Load test data from CSV files
- **JSON Data Files** - Load test data from JSON files

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
│   │   │   └── com/westfieldgrp/testing/api
│   │   │       ├── auth/
│   │   │       │   └── AuthenticationService.java
│   │   │       └── config/
│   │   │           └── TestConfig.java
│   |   |       └── BaseApiTest.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/westfieldgrp/testing/api
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

2. Use `authenticatedRequest()` for authenticated API calls or `unauthenticatedRequest()` for unauthenticated calls:
```java
@Test
public void testMyEndpoint() {
    // Using fluent API (REST-assured-like syntax) with authentication
    APIResponse response = authenticatedRequest()
        .get("/my/endpoint")
        .then()
        .statusCode(200)
        .extract()
        .response();
    
    // For unauthenticated requests (testing unauthorized access, etc.)
    APIResponse unauthenticatedResponse = unauthenticatedRequest()
        .get("/my/endpoint")
        .then()
        .statusCode(401, 403) // Accept multiple status codes
        .extract()
        .response();
    
    // Or using direct helper methods
    APIResponse response2 = get("/my/endpoint");
    assertStatus(response2, 200);
}
```

### Authentication Flow

The framework automatically:
1. Retrieves JWT token from PING Federate using client credentials
2. Caches the token for 55 minutes (to avoid excessive token requests)
3. Adds the token to all requests via `Authorization: Bearer <token>` header

### Customizing API Request Context

Override methods in your test class:

```java
@Override
protected void customizeApiRequestContext(APIRequest.NewContextOptions options) {
    options.setExtraHTTPHeaders(Map.of("X-Custom-Header", "value"));
}
```

### Generating JSON Request Bodies with Velocity Templates

The framework includes a `TemplateService` for generating JSON request bodies from Velocity templates. This is useful for creating dynamic request payloads for POST and PUT requests.

#### Creating Templates

Create Velocity template files (`.vm` extension) in `src/main/resources/templates/` or `src/test/resources/templates/`:

**Example: `templates/user-create.json.vm`**
```json
{
  "firstName": "$firstName",
  "lastName": "$lastName",
  "email": "$email",
  "age": $age
#if($phoneNumber)
  ,"phoneNumber": "$phoneNumber"
#end
}
```

#### Using Templates in Tests

**Method 1: Using Map context**
```java
@Test
public void testCreateUser() {
    Map<String, Object> context = new HashMap<>();
    context.put("firstName", "John");
    context.put("lastName", "Doe");
    context.put("email", "john.doe@example.com");
    context.put("age", 30);
    context.put("phoneNumber", "555-1234");
    
    String jsonBody = renderTemplate("templates/user-create.json.vm", context);
    
    // Using fluent API
    APIResponse response = authenticatedRequest()
        .post("/users", jsonBody)
        .then()
        .statusCode(201)
        .extract()
        .response();
    
    // Or using direct helper
    APIResponse response2 = post("/users", jsonBody);
    assertStatus(response2, 201);
}
```

**Method 2: Using varargs (convenient for simple cases)**
```java
@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John",
        "lastName", "Doe",
        "email", "john.doe@example.com",
        "age", 30,
        "phoneNumber", "555-1234");
    
    APIResponse response = authenticatedRequest()
        .post("/users", jsonBody)
        .then()
        .statusCode(201)
        .extract()
        .response();
}
```

#### Velocity Template Features

The templates support all Velocity template language features:

- **Variables**: `$variableName`
- **Conditionals**: `#if($condition) ... #end`
- **Loops**: `#foreach($item in $list) ... #end`
- **Complex objects**: Access nested properties

**Example with list:**
```json
{
  "name": "$productName",
  "tags": [
#foreach($tag in $tags)
    "$tag"#if($foreach.hasNext),#end
#end
  ]
}
```

#### Template Caching

Templates are automatically cached after first load for better performance. To clear the cache (useful in tests):

```java
getTemplateService().clearCache(); // Clear all templates
getTemplateService().clearCache("templates/user-create.json.vm"); // Clear specific template
```

#### Using JSON Data Files

You can load test data from JSON files to use as context for your templates. This is useful for:
- Storing test data in JSON files with arrays of values
- Reusing test data across multiple templates
- Separating test data from template structure

**Example: Data file (`templates/user-data.json`)**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "age": 30,
  "phoneNumber": "555-1234",
  "address": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "12345"
  },
  "tags": ["customer", "premium", "active"]
}
```

**Usage in test:**
```java
@Test
public void testCreateUserWithDataFile() {
    // Load data from JSON file and use it as context for the template
    String jsonBody = renderTemplate(
        "templates/user-create.json.vm",  // Main template
        "templates/user-data.json"         // JSON data file
    );
    
    // Using fluent API
    APIResponse response = authenticatedRequest()
        .post("/users", jsonBody)
        .then()
        .statusCode(201)
        .extract()
        .response();
    
    // Or using direct helper
    APIResponse response2 = post("/users", jsonBody);
    assertStatus(response2, 201);
}
```

**With additional context to override values:**
```java
@Test
public void testCreateUserWithOverrides() {
    // Load data from JSON file, but override specific values
    String jsonBody = renderTemplate(
        "templates/user-create.json.vm",
        "templates/user-data.json",
        "firstName", "Jane",  // Override firstName from JSON file
        "age", 25              // Override age from JSON file
    );
    
    APIResponse response = authenticatedRequest()
        .post("/users", jsonBody)
        .then()
        .statusCode(201)
        .extract()
        .response();
}
```

**Using CSV data files:**
```java
@Test
public void testCreateUserFromCsv() {
    // Load first row from CSV file
    Map<String, String> userData = loadCsvAsMap("templates/user-data.csv");
    
    // Use as context for template
    String jsonBody = renderTemplate("templates/user-create.json.vm", userData);
    
    APIResponse response = authenticatedRequest()
        .post("/users", jsonBody)
        .then()
        .statusCode(201)
        .extract()
        .response();
}

@Test
public void testCreateMultipleUsersFromCsv() {
    // Load all rows from CSV
    List<Map<String, String>> allUsers = loadCsvAsList("templates/user-data.csv");
    
    for (Map<String, String> userData : allUsers) {
        String jsonBody = renderTemplate("templates/user-create.json.vm", userData);
        APIResponse response = post("/users", jsonBody);
        assertStatus(response, 201);
    }
}
```

The JSON file is loaded from the classpath, parsed, and merged with any additional context (additional context takes precedence over JSON file values).

#### Advanced Usage

For advanced template operations, access the `TemplateService` directly:

```java
TemplateService templateService = getTemplateService();
String json = templateService.render("templates/complex.json.vm", context);
```

## Example Test

See `ExampleApiTest.java` for complete examples including:
- GET endpoint testing
- POST endpoint testing
- Unauthorized access testing
- Response validation

## Dependencies

- **Playwright 1.40.0** - API and browser testing
- **JUnit 5.10.1** - Testing framework
- **Lombok 1.18.30** - Code generation
- **Jackson 2.16.1** - JSON processing
- **Apache Velocity 2.3** - Template engine for JSON generation
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

### Template Not Found or Rendering Fails

1. Verify template file exists in `src/main/resources/templates/` or `src/test/resources/templates/`
2. Check template path is correct (case-sensitive)
3. Ensure template file has `.vm` extension
4. Verify all required variables are provided in context
5. Check template syntax (Velocity syntax errors will cause rendering to fail)
6. Review logs for detailed error messages

## Best Practices

1. **Token Management**: The framework caches tokens automatically. Use `authService.invalidateToken()` if you need to force token refresh.

2. **Test Isolation**: Each test gets a fresh request specification with authentication.

3. **Error Handling**: Always validate response status codes and check response bodies for error details.

4. **Logging**: Use the provided SLF4J logging for debugging test execution.

5. **Configuration**: Prefer environment variables for sensitive data (client secrets) over properties files.

6. **Template Organization**: Organize templates by domain or feature (e.g., `templates/users/`, `templates/products/`).

7. **Template Reusability**: Create base templates for common structures and use includes or composition for variations.

## License

This is a proprietary testing framework for internal use.

## Support

## pom.xml needs
```
<dependency>
    <groupId>com.westfieldgrp.testing</groupId>
    <artifactId>rest-api-testing</artifactId>
    <version>3.0.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
</dependency>
```
## Example Usage for Java
```Java
package com.westfieldgrp.testing.api;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.westfieldgrp.testing.api.auth.OAuthScopes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        // Use unauthenticatedRequest() helper for requests without authentication
        APIResponse response = unauthenticatedRequest()
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
            .then()
            .statusCode(401, 403) // Accept either 401 or 403
            .extract()
            .response();

        log.info("Response status: {}", response.status());
        assertTrue(
            response.status() == 401 || response.status() == 403,
            "Expected 401 or 403 status code for unauthorized access"
        );
    }

    @Test
    @DisplayName("Test Authorization Fails with incorrect OAuth scopes")
    @OAuthScopes({""})
    public void testAuthorizationFailsWithIncorrectScopes() {
        log.info("Testing authorization fails with incorrect OAuth scopes");

        authenticatedRequest()
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("Test GET endpoint - Example")
    public void testGetEndpoint() {
        log.info("Executing GET endpoint test");

        // Using fluent API
        APIResponse response = authenticatedRequest()
            .get(this.endpoint + "?addressLine1=Main St&state=OH&postalCode=44212&city=BRUNSWICK")
            .then()
            .statusCode(200)
            .body("inCity", notNull())
            .body("inState", not(emptyString()))
            .extract()
            .response();

        log.info("Response status: {}", response.status());
        log.info("Response body: {}", response.text());

        assertNotNull(response);
        assertEquals(200, response.status());
    }

    @Test
    @DisplayName("Test healthcheck endpoint validation")
    public void testEndpointValidation() {
        log.info("Testing endpoint response validation");

        authenticatedRequest()
            .get("/dom-geocode-v1/actuator/health")
            .then()
            .statusCode(200)
            .body("status", notNull());
    }
    
}
```