# Playwright vs REST-assured Comparison

This document shows how the library would look if using Playwright instead of REST-assured for API testing.

## Key Differences

### 1. Dependencies (pom.xml)

**REST-assured:**
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
</dependency>
```

**Playwright:**
```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
</dependency>
```

---

## 2. BaseApiTest Implementation

### REST-assured Version (Current)
```java
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

protected RequestSpecification requestSpec;

@BeforeEach
public void setUpTest(TestInfo testInfo) {
    String accessToken = authService.getAccessToken(scopes, bypassCache);
    
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
        .setBaseUri(config.getApiBaseUrl())
        .setContentType(ContentType.JSON)
        .addHeader("Authorization", "Bearer " + accessToken);
    
    requestSpec = requestSpecBuilder.build();
}

protected RequestSpecification getAuthenticatedRequest() {
    return RestAssured.given(requestSpec);
}
```

### Playwright Version
```java
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

protected static Playwright playwright;
protected static APIRequestContext apiRequestContext;

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
    
    String[] scopes = extractScopesFromAnnotation(testInfo);
    boolean bypassCache = shouldBypassCache(testInfo);
    String accessToken = authService.getAccessToken(scopes, bypassCache);
    
    // Create API request context with base configuration
    APIRequest.NewContextOptions contextOptions = new APIRequest.NewContextOptions()
        .setBaseURL(config.getApiBaseUrl())
        .setExtraHTTPHeaders(Map.of(
            "Authorization", "Bearer " + accessToken,
            "Content-Type", "application/json"
        ));
    
    // Allow customization
    customizeApiRequestContext(contextOptions);
    
    apiRequestContext = playwright.request().newContext(contextOptions);
    
    log.info("Test setup completed");
}

/**
 * Override this method to customize API request context for specific test classes
 */
protected void customizeApiRequestContext(APIRequest.NewContextOptions options) {
    // Default implementation - override in subclasses if needed
}

/**
 * Helper method to get authenticated API request context
 */
protected APIRequestContext getApiRequestContext() {
    return apiRequestContext;
}

@AfterAll
public static void tearDownTestSuite() {
    if (apiRequestContext != null) {
        apiRequestContext.dispose();
    }
    if (playwright != null) {
        playwright.close();
    }
}
```

---

## 3. Test Examples

### REST-assured Version (Current)
```java
@Test
public void testGetUser() {
    Response response = getAuthenticatedRequest()
        .when()
        .get("/users/123")
        .then()
        .spec(responseSpec)
        .statusCode(200)
        .body("firstName", equalTo("John"))
        .extract()
        .response();
    
    String userId = response.jsonPath().getString("id");
    assertEquals("123", userId);
}

@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John",
        "lastName", "Doe");
    
    Response response = getAuthenticatedRequest()
        .body(jsonBody)
        .when()
        .post("/users")
        .then()
        .spec(responseSpec)
        .statusCode(201)
        .extract()
        .response();
}
```

### Playwright Version
```java
import com.microsoft.playwright.APIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@Test
public void testGetUser() {
    APIResponse response = getApiRequestContext()
        .get("/users/123");
    
    assertEquals(200, response.status());
    
    JsonNode json = new ObjectMapper().readTree(response.text());
    assertEquals("John", json.get("firstName").asText());
    assertEquals("123", json.get("id").asText());
}

@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John",
        "lastName", "Doe");
    
    APIResponse response = getApiRequestContext()
        .post("/users", 
            RequestOptions.create()
                .setData(jsonBody));
    
    assertEquals(201, response.status());
    
    JsonNode json = new ObjectMapper().readTree(response.text());
    assertNotNull(json.get("id"));
}
```

---

## 4. Response Handling Helper Methods

### REST-assured Version
```java
// Built-in fluent API
.then()
.statusCode(200)
.body("field", equalTo("value"))
```

### Playwright Version (Helper Methods)
```java
/**
 * Helper method to assert response status
 */
protected void assertStatus(APIResponse response, int expectedStatus) {
    assertEquals(expectedStatus, response.status(), 
        "Expected status " + expectedStatus + " but got " + response.status());
}

/**
 * Helper method to parse JSON response
 */
protected JsonNode parseJsonResponse(APIResponse response) {
    try {
        return new ObjectMapper().readTree(response.text());
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse JSON response", e);
    }
}

/**
 * Helper method to assert JSON field value
 */
protected void assertJsonField(APIResponse response, String fieldPath, String expectedValue) {
    JsonNode json = parseJsonResponse(response);
    String actualValue = json.at(fieldPath).asText();
    assertEquals(expectedValue, actualValue);
}

// Usage:
@Test
public void testGetUser() {
    APIResponse response = getApiRequestContext().get("/users/123");
    assertStatus(response, 200);
    assertJsonField(response, "/firstName", "John");
}
```

---

## 5. Request Builder Pattern (Optional Enhancement)

You could create a fluent builder similar to REST-assured:

```java
public class ApiRequestBuilder {
    private final APIRequestContext context;
    private String method;
    private String url;
    private Object data;
    private Map<String, String> headers = new HashMap<>();
    
    public ApiRequestBuilder(APIRequestContext context) {
        this.context = context;
    }
    
    public ApiRequestBuilder get(String url) {
        this.method = "GET";
        this.url = url;
        return this;
    }
    
    public ApiRequestBuilder post(String url) {
        this.method = "POST";
        this.url = url;
        return this;
    }
    
    public ApiRequestBuilder body(String jsonBody) {
        this.data = jsonBody;
        return this;
    }
    
    public ApiRequestBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }
    
    public APIResponse execute() {
        RequestOptions options = RequestOptions.create();
        if (data != null) {
            options.setData(data.toString());
        }
        if (!headers.isEmpty()) {
            options.setHeaders(headers);
        }
        
        return switch (method) {
            case "GET" -> context.get(url, options);
            case "POST" -> context.post(url, options);
            case "PUT" -> context.put(url, options);
            case "DELETE" -> context.delete(url, options);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }
}

// Usage:
@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John");
    
    APIResponse response = new ApiRequestBuilder(getApiRequestContext())
        .post("/users")
        .body(jsonBody)
        .execute();
    
    assertStatus(response, 201);
}
```

---

## 6. Complete BaseApiTest with Playwright

```java
package com.wfld.testing.api;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.wfld.testing.api.auth.AuthenticationService;
import com.wfld.testing.api.auth.BypassTokenCache;
import com.wfld.testing.api.auth.OAuthScopes;
import com.wfld.testing.api.config.TestConfig;
import com.wfld.testing.api.template.TemplateService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Getter
public abstract class BaseApiTest {
    protected static TestConfig config;
    protected static AuthenticationService authService;
    protected static TemplateService templateService;
    protected static Playwright playwright;
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

        String[] scopes = extractScopesFromAnnotation(testInfo);
        boolean bypassCache = shouldBypassCache(testInfo);
        String accessToken = authService.getAccessToken(scopes, bypassCache);

        APIRequest.NewContextOptions contextOptions = new APIRequest.NewContextOptions()
            .setBaseURL(config.getApiBaseUrl())
            .setExtraHTTPHeaders(Map.of(
                "Authorization", "Bearer " + accessToken,
                "Content-Type", "application/json"
            ));

        customizeApiRequestContext(contextOptions);
        apiRequestContext = playwright.request().newContext(contextOptions);

        log.info("Test setup completed");
    }

    @AfterAll
    public static void tearDownTestSuite() {
        if (playwright != null) {
            playwright.close();
        }
    }

    protected void customizeApiRequestContext(APIRequest.NewContextOptions options) {
        // Default implementation - override in subclasses if needed
    }

    protected APIRequestContext getApiRequestContext() {
        return apiRequestContext;
    }

    // Template methods remain the same...
    protected String renderTemplate(String templatePath, Map<String, Object> context) {
        return templateService.render(templatePath, context);
    }

    // ... rest of template methods and annotation extraction methods remain the same
}
```

---

## Summary of Key Differences

| Feature | REST-assured | Playwright |
|---------|-------------|------------|
| **Fluent API** | Built-in fluent DSL | Need to build custom helpers |
| **Response Validation** | `.then().statusCode(200)` | Manual assertions or helper methods |
| **JSON Path** | Built-in `.jsonPath()` | Use Jackson ObjectMapper |
| **Request Specs** | `RequestSpecification` | `APIRequest.NewContextOptions` |
| **Response Specs** | `ResponseSpecification` | Manual validation |
| **Setup** | Static configuration | Context-based per test |
| **Type Safety** | Less type-safe | More type-safe with Jackson |
| **Browser Support** | API only | API + Browser automation |
| **Learning Curve** | Steeper (DSL) | More straightforward (HTTP-like) |

## Pros and Cons

### REST-assured Pros:
- ✅ Fluent, readable DSL
- ✅ Built-in JSON path extraction
- ✅ Response specifications
- ✅ Mature API testing library
- ✅ Great for REST API testing

### REST-assured Cons:
- ❌ API testing only
- ❌ Can be verbose for simple requests
- ❌ Less flexible for complex scenarios

### Playwright Pros:
- ✅ Can do both API and browser testing
- ✅ More control over requests
- ✅ Better for complex scenarios
- ✅ Modern, actively maintained
- ✅ Type-safe with proper JSON parsing

### Playwright Cons:
- ❌ More boilerplate for simple requests
- ❌ Need to build helper methods for fluent API
- ❌ Less specialized for pure API testing
- ❌ Response validation requires more code

