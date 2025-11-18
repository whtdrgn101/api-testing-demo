# Side-by-Side Test Examples: REST-assured vs Playwright

## Example 1: Simple GET Request

### REST-assured (Current)
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
        .body("lastName", equalTo("Doe"))
        .extract()
        .response();
    
    String userId = response.jsonPath().getString("id");
    assertEquals("123", userId);
}
```

### Playwright
```java
@Test
public void testGetUser() {
    APIResponse response = get("/users/123");
    
    assertStatus(response, 200);
    assertJsonField(response, "/firstName", "John");
    assertJsonField(response, "/lastName", "Doe");
    
    JsonNode json = parseJsonResponse(response);
    assertEquals("123", json.get("id").asText());
}
```

---

## Example 2: POST Request with Template

### REST-assured (Current)
```java
@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John",
        "lastName", "Doe",
        "email", "john.doe@example.com");
    
    Response response = getAuthenticatedRequest()
        .body(jsonBody)
        .when()
        .post("/users")
        .then()
        .spec(responseSpec)
        .statusCode(201)
        .body("id", notNullValue())
        .body("firstName", equalTo("John"))
        .extract()
        .response();
    
    String userId = response.jsonPath().getString("id");
    assertNotNull(userId);
}
```

### Playwright
```java
@Test
public void testCreateUser() {
    String jsonBody = renderTemplate("templates/user-create.json.vm",
        "firstName", "John",
        "lastName", "Doe",
        "email", "john.doe@example.com");
    
    APIResponse response = post("/users", jsonBody);
    
    assertStatus(response, 201);
    
    JsonNode json = parseJsonResponse(response);
    assertNotNull(json.get("id"));
    assertEquals("John", json.get("firstName").asText());
}
```

---

## Example 3: POST with JSON Data File

### REST-assured (Current)
```java
@Test
public void testCreateUserWithDataFile() {
    String jsonBody = renderTemplate(
        "templates/user-create.json.vm",
        "templates/user-data.json"
    );
    
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

### Playwright
```java
@Test
public void testCreateUserWithDataFile() {
    String jsonBody = renderTemplate(
        "templates/user-create.json.vm",
        "templates/user-data.json"
    );
    
    APIResponse response = post("/users", jsonBody);
    assertStatus(response, 201);
}
```

---

## Example 4: PUT Request with Overrides

### REST-assured (Current)
```java
@Test
public void testUpdateUser() {
    String jsonBody = renderTemplate(
        "templates/user-update.json.vm",
        "templates/user-data.json",
        "firstName", "Jane",  // Override
        "age", 25              // Override
    );
    
    Response response = getAuthenticatedRequest()
        .body(jsonBody)
        .when()
        .put("/users/123")
        .then()
        .spec(responseSpec)
        .statusCode(200)
        .body("firstName", equalTo("Jane"))
        .body("age", equalTo(25))
        .extract()
        .response();
}
```

### Playwright
```java
@Test
public void testUpdateUser() {
    String jsonBody = renderTemplate(
        "templates/user-update.json.vm",
        "templates/user-data.json",
        "firstName", "Jane",
        "age", 25
    );
    
    APIResponse response = put("/users/123", jsonBody);
    
    assertStatus(response, 200);
    
    JsonNode json = parseJsonResponse(response);
    assertEquals("Jane", json.get("firstName").asText());
    assertEquals(25, json.get("age").asInt());
}
```

---

## Example 5: DELETE Request

### REST-assured (Current)
```java
@Test
public void testDeleteUser() {
    getAuthenticatedRequest()
        .when()
        .delete("/users/123")
        .then()
        .spec(responseSpec)
        .statusCode(204);
}
```

### Playwright
```java
@Test
public void testDeleteUser() {
    APIResponse response = delete("/users/123");
    assertStatus(response, 204);
}
```

---

## Example 6: Complex Validation

### REST-assured (Current)
```java
@Test
public void testGetUserWithComplexValidation() {
    Response response = getAuthenticatedRequest()
        .when()
        .get("/users/123")
        .then()
        .spec(responseSpec)
        .statusCode(200)
        .body("firstName", equalTo("John"))
        .body("address.city", equalTo("Anytown"))
        .body("tags", hasSize(3))
        .body("tags", hasItem("premium"))
        .extract()
        .response();
    
    // Access nested fields
    String city = response.jsonPath().getString("address.city");
    List<String> tags = response.jsonPath().getList("tags");
    
    assertEquals("Anytown", city);
    assertTrue(tags.contains("premium"));
}
```

### Playwright
```java
@Test
public void testGetUserWithComplexValidation() {
    APIResponse response = get("/users/123");
    
    assertStatus(response, 200);
    
    JsonNode json = parseJsonResponse(response);
    
    // Simple field validation
    assertEquals("John", json.get("firstName").asText());
    
    // Nested field validation
    assertEquals("Anytown", json.get("address").get("city").asText());
    
    // Array validation
    JsonNode tags = json.get("tags");
    assertEquals(3, tags.size());
    
    // Check array contains value
    boolean hasPremium = false;
    for (JsonNode tag : tags) {
        if ("premium".equals(tag.asText())) {
            hasPremium = true;
            break;
        }
    }
    assertTrue(hasPremium);
    
    // Or using streams (Java 8+)
    List<String> tagList = new ArrayList<>();
    tags.forEach(tag -> tagList.add(tag.asText()));
    assertTrue(tagList.contains("premium"));
}
```

---

## Example 7: Custom Headers

### REST-assured (Current)
```java
@Test
public void testWithCustomHeader() {
    Response response = getAuthenticatedRequest()
        .header("X-Custom-Header", "custom-value")
        .when()
        .get("/users/123")
        .then()
        .spec(responseSpec)
        .statusCode(200)
        .extract()
        .response();
}
```

### Playwright
```java
@Test
public void testWithCustomHeader() {
    APIResponse response = getApiRequestContext()
        .get("/users/123", 
            RequestOptions.create()
                .setHeader("X-Custom-Header", "custom-value"));
    
    assertStatus(response, 200);
}
```

---

## Example 8: Error Handling

### REST-assured (Current)
```java
@Test
public void testNotFound() {
    getAuthenticatedRequest()
        .when()
        .get("/users/999")
        .then()
        .spec(responseSpec)
        .statusCode(404)
        .body("error", equalTo("User not found"));
}
```

### Playwright
```java
@Test
public void testNotFound() {
    APIResponse response = get("/users/999");
    
    assertStatus(response, 404);
    
    JsonNode json = parseJsonResponse(response);
    assertEquals("User not found", json.get("error").asText());
}
```

---

## Summary

### REST-assured Advantages:
- ✅ More concise for simple validations
- ✅ Built-in fluent assertions
- ✅ Less boilerplate for common cases
- ✅ JSON path extraction is simpler

### Playwright Advantages:
- ✅ More explicit and clear
- ✅ Better type safety with Jackson
- ✅ More control over requests
- ✅ Can extend to browser testing
- ✅ More standard Java patterns

### When to Use Each:

**Use REST-assured if:**
- You're doing pure API testing
- You want the most concise syntax
- You prefer fluent DSLs
- Your team is familiar with REST-assured

**Use Playwright if:**
- You need both API and browser testing
- You want more control and explicitness
- You prefer standard Java patterns
- You want better type safety
- You're building a unified testing framework

