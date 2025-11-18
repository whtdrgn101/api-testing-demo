# Playwright with Fluent API (REST-assured-like)

Yes! Playwright can work with a fluent API similar to REST-assured. You just need to create a wrapper class that provides the fluent DSL. Here's how:

## Fluent API Wrapper

I've created `PlaywrightApiRequest` which provides a REST-assured-like fluent API for Playwright. Here's how it works:

## Usage Examples

### Basic GET Request

**REST-assured:**
```java
Response response = getAuthenticatedRequest()
    .when()
    .get("/users/123")
    .then()
    .statusCode(200)
    .body("firstName", equalTo("John"))
    .extract()
    .response();
```

**Playwright with Fluent API:**
```java
APIResponse response = new PlaywrightApiRequest(getApiRequestContext())
    .get("/users/123")
    .then()
    .statusCode(200)
    .body("firstName", equalTo("John"))
    .extract()
    .response();
```

### POST Request with Body

**REST-assured:**
```java
String jsonBody = renderTemplate("templates/user-create.json.vm", context);

Response response = getAuthenticatedRequest()
    .body(jsonBody)
    .when()
    .post("/users")
    .then()
    .statusCode(201)
    .body("id", notNullValue())
    .extract()
    .response();
```

**Playwright with Fluent API:**
```java
String jsonBody = renderTemplate("templates/user-create.json.vm", context);

APIResponse response = new PlaywrightApiRequest(getApiRequestContext())
    .post("/users")
    .body(jsonBody)
    .then()
    .statusCode(201)
    .body("id", notNullValue())
    .extract()
    .response();
```

### With Custom Headers

**REST-assured:**
```java
Response response = getAuthenticatedRequest()
    .header("X-Custom-Header", "value")
    .when()
    .get("/users/123")
    .then()
    .statusCode(200)
    .extract()
    .response();
```

**Playwright with Fluent API:**
```java
APIResponse response = new PlaywrightApiRequest(getApiRequestContext())
    .get("/users/123")
    .header("X-Custom-Header", "value")
    .then()
    .statusCode(200)
    .extract()
    .response();
```

### JSON Path Extraction

**REST-assured:**
```java
String userId = response.jsonPath().getString("id");
List<String> tags = response.jsonPath().getList("tags");
```

**Playwright with Fluent API:**
```java
String userId = new PlaywrightApiRequest(getApiRequestContext())
    .get("/users/123")
    .extract()
    .path("id");

List<String> tags = new PlaywrightApiRequest(getApiRequestContext())
    .get("/users/123")
    .extract()
    .pathAsList("tags", String.class);
```

## Integration with BaseApiTest

You can add a helper method to `BaseApiTest` to make it even more convenient:

```java
protected PlaywrightApiRequest request() {
    return new PlaywrightApiRequest(getApiRequestContext());
}
```

Then your tests become:

```java
@Test
public void testGetUser() {
    APIResponse response = request()
        .get("/users/123")
        .then()
        .statusCode(200)
        .body("firstName", equalTo("John"))
        .extract()
        .response();
}

@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John");

    APIResponse response = request()
        .post("/users")
        .body(jsonBody)
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .extract()
        .response();
}
```

## Available Methods

### Request Building
- `get(String url)` - GET request
- `post(String url)` - POST request
- `put(String url)` - PUT request
- `delete(String url)` - DELETE request
- `patch(String url)` - PATCH request
- `body(String jsonBody)` - Set request body (JSON string)
- `body(Object object)` - Set request body (auto-serialized to JSON)
- `header(String name, String value)` - Add header
- `headers(Map<String, String> headers)` - Add multiple headers
- `queryParam(String name, String value)` - Add query parameter
- `queryParams(Map<String, String> params)` - Add multiple query parameters

### Response Validation (`.then()`)
- `statusCode(int expected)` - Assert status code
- `body(String jsonPath, Object expectedValue)` - Assert JSON field value
- `body(String jsonPath, Consumer<JsonNode> validator)` - Custom validation
- `contentType(String expected)` - Assert content type
- `header(String name, String expectedValue)` - Assert header value

### Response Extraction (`.extract()`)
- `response()` - Get full APIResponse
- `asString()` - Get response as string
- `asJson()` - Get response as JsonNode
- `as(Class<T> clazz)` - Deserialize to object
- `path(String jsonPath)` - Extract field by JSON path
- `pathAsList(String jsonPath, Class<T> elementClass)` - Extract array as List

## Matchers

Similar to REST-assured's Hamcrest matchers, you can use:

```java
import static com.wfld.testing.api.playwright.PlaywrightMatchers.*;

request()
    .get("/users/123")
    .then()
    .body("age", greaterThan(18))
    .body("tags", hasSize(3))
    .body("tags", hasItem("premium"));
```

Available matchers:
- `equalTo(Object)` - Equality check
- `notNullValue()` - Not null check
- `nullValue()` - Null check
- `containsString(String)` - String contains
- `greaterThan(Number)` - Number comparison
- `lessThan(Number)` - Number comparison
- `hasSize(int)` - Array size check
- `hasItem(Object)` - Array contains item

## Complete Example

```java
public class UserApiTest extends BaseApiTestPlaywrightExample {
    
    @Test
    public void testGetUser() {
        APIResponse response = request()
            .get("/users/123")
            .then()
            .statusCode(200)
            .body("firstName", equalTo("John"))
            .body("lastName", equalTo("Doe"))
            .body("age", greaterThan(18))
            .extract()
            .response();
        
        String userId = request()
            .get("/users/123")
            .extract()
            .path("id");
        
        assertEquals("123", userId);
    }
    
    @Test
    public void testCreateUser() {
        String jsonBody = renderTemplate("templates/user-create.json.vm",
            "firstName", "John",
            "lastName", "Doe");
        
        APIResponse response = request()
            .post("/users")
            .body(jsonBody)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("firstName", equalTo("John"))
            .extract()
            .response();
    }
    
    @Test
    public void testUpdateUser() {
        String jsonBody = renderTemplate("templates/user-update.json.vm",
            "templates/user-data.json",
            "firstName", "Jane");
        
        request()
            .put("/users/123")
            .body(jsonBody)
            .then()
            .statusCode(200)
            .body("firstName", equalTo("Jane"));
    }
}
```

## Benefits

✅ **Fluent API** - Same style as REST-assured
✅ **Type Safety** - Better type safety with Jackson
✅ **Extensible** - Easy to add custom validators
✅ **Playwright Benefits** - Still get Playwright's features (browser testing, etc.)
✅ **Familiar Syntax** - Easy migration from REST-assured

## Summary

Yes, Playwright can absolutely work with a fluent API like REST-assured! The wrapper class provides:

1. **Same fluent syntax** as REST-assured
2. **All the same features** (status codes, body validation, extraction)
3. **Better type safety** with Jackson
4. **Easy to extend** with custom validators
5. **Can still use Playwright's native API** when needed

The main difference is you need to instantiate `PlaywrightApiRequest` instead of using static methods, but with a helper method in `BaseApiTest`, the syntax becomes nearly identical to REST-assured.

