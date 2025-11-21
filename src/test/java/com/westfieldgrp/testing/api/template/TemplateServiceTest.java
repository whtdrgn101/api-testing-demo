package com.westfieldgrp.testing.api.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService Tests")
class TemplateServiceTest {

    private TemplateService templateService;

    @BeforeEach
    void setUp() throws Exception {
        // Clear singleton instance using reflection
        Field instanceField = TemplateService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        templateService = TemplateService.getInstance();
        // Clear cache before each test
        templateService.clearCache();
    }

    @Test
    @DisplayName("Test simple template rendering with Map context")
    void testSimpleTemplateRendering() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "John Doe");
        context.put("value", 42);

        String result = templateService.render("templates/test-simple.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    @DisplayName("Test template rendering with varargs context")
    void testTemplateRenderingWithVarargs() {
        // Explicitly call the 2-parameter version by casting to avoid ambiguity
        String result = templateService.render("templates/test-simple.json.vm",
            (Object[]) new Object[]{"name", "Jane Doe", "value", 100});

        assertNotNull(result);
        assertTrue(result.contains("Jane Doe"));
        assertTrue(result.contains("100"));
    }

    @Test
    @DisplayName("Test template with conditional logic")
    void testConditionalTemplate() {
        Map<String, Object> context = new HashMap<>();
        context.put("id", "123");
        context.put("required", "required-value");
        context.put("optional", "optional-value");

        String result = templateService.render("templates/test-conditional.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"123\""));
        assertTrue(result.contains("\"optional\""));
        assertTrue(result.contains("\"optional-value\""));
        assertTrue(result.contains("\"required\""));
    }

    @Test
    @DisplayName("Test template with conditional logic - optional field missing")
    void testConditionalTemplateWithoutOptional() {
        Map<String, Object> context = new HashMap<>();
        context.put("id", "123");
        context.put("required", "required-value");

        String result = templateService.render("templates/test-conditional.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"123\""));
        assertTrue(result.contains("\"required\""));
        assertFalse(result.contains("\"optional\""));
    }

    @Test
    @DisplayName("Test template with list iteration")
    void testTemplateWithList() {
        Map<String, Object> context = new HashMap<>();
        List<String> items = Arrays.asList("item1", "item2", "item3");
        context.put("items", items);

        String result = templateService.render("templates/test-list.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("\"items\""));
        assertTrue(result.contains("\"item1\""));
        assertTrue(result.contains("\"item2\""));
        assertTrue(result.contains("\"item3\""));
    }

    @Test
    @DisplayName("Test template caching - same template loaded twice should use cache")
    void testTemplateCaching() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");
        context.put("value", 1);

        // First render - should load template
        String result1 = templateService.render("templates/test-simple.json.vm", context);
        int cacheSize1 = templateService.getCacheSize();

        // Second render - should use cached template
        String result2 = templateService.render("templates/test-simple.json.vm", context);
        int cacheSize2 = templateService.getCacheSize();

        assertEquals(result1, result2);
        assertEquals(cacheSize1, cacheSize2);
        assertEquals(1, cacheSize1); // Only one template cached
    }

    @Test
    @DisplayName("Test template cache clear")
    void testTemplateCacheClear() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");
        context.put("value", 1);

        templateService.render("templates/test-simple.json.vm", context);
        assertEquals(1, templateService.getCacheSize());

        templateService.clearCache();
        assertEquals(0, templateService.getCacheSize());
    }

    @Test
    @DisplayName("Test template cache clear for specific template")
    void testTemplateCacheClearSpecific() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");
        context.put("value", 1);

        templateService.render("templates/test-simple.json.vm", context);
        templateService.render("templates/test-conditional.json.vm", context);
        assertEquals(2, templateService.getCacheSize());

        templateService.clearCache("templates/test-simple.json.vm");
        assertEquals(1, templateService.getCacheSize());
    }

    @Test
    @DisplayName("Test template rendering with null context")
    void testTemplateRenderingWithNullContext() {
        String result = templateService.render("templates/test-simple.json.vm", (Map<String, Object>) null);

        assertNotNull(result);
        // Template should still render, just without variable substitution
        assertTrue(result.contains("\"name\""));
    }

    @Test
    @DisplayName("Test template rendering with empty context")
    void testTemplateRenderingWithEmptyContext() {
        Map<String, Object> context = new HashMap<>();
        String result = templateService.render("templates/test-simple.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("\"name\""));
    }

    @Test
    @DisplayName("Test template not found throws exception")
    void testTemplateNotFound() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render("templates/non-existent.json.vm", context);
        });
    }

    @Test
    @DisplayName("Test null template path throws exception")
    void testNullTemplatePath() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render(null, context);
        });
    }

    @Test
    @DisplayName("Test empty template path throws exception")
    void testEmptyTemplatePath() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render("", context);
        });
    }

    @Test
    @DisplayName("Test varargs with odd number of arguments throws exception")
    void testVarargsOddArguments() {
        // Explicitly call the 2-parameter version to avoid ambiguity with 3-parameter version
        assertThrows(IllegalArgumentException.class, () -> {
            templateService.render("templates/test-simple.json.vm", 
                (Object[]) new Object[]{"key1", "value1", "key2"});
        });
    }

    @Test
    @DisplayName("Test varargs with non-string keys throws exception")
    void testVarargsNonStringKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            templateService.render("templates/test-simple.json.vm", 123, "value1");
        });
    }

    @Test
    @DisplayName("Test singleton pattern")
    void testSingletonPattern() {
        TemplateService instance1 = TemplateService.getInstance();
        TemplateService instance2 = TemplateService.getInstance();

        assertSame(instance1, instance2, "Should return same instance");
    }

    @Test
    @DisplayName("Test template with numeric values")
    void testTemplateWithNumericValues() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Product");
        context.put("value", 99.99);

        String result = templateService.render("templates/test-simple.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("99.99"));
    }

    @Test
    @DisplayName("Test template with boolean values")
    void testTemplateWithBooleanValues() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Test");
        context.put("value", true);

        String result = templateService.render("templates/test-simple.json.vm", context);

        assertNotNull(result);
        assertTrue(result.contains("true"));
    }

    @Test
    @DisplayName("Test rendering template with data file - Map context")
    void testRenderWithDataFile() {
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("name", "Override");

        String result = templateService.render(
            "templates/test-simple.json.vm",
            "templates/test-data.json",
            additionalContext
        );

        assertNotNull(result);
        // The data file provides context with name, value, computed, items, metadata
        // Additional context can override values
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"value\""));
        // Additional context should override the JSON file value
        assertTrue(result.contains("Override"));
    }

    @Test
    @DisplayName("Test rendering template with data file - varargs context")
    void testRenderWithDataFileVarargs() {
        String result = templateService.render(
            "templates/test-simple.json.vm",
            "templates/test-data.json",
            "name", "Override Item",
            "value", 42
        );

        assertNotNull(result);
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"value\""));
        // Additional context should override JSON file values
        assertTrue(result.contains("Override Item"));
    }

    @Test
    @DisplayName("Test data file loads valid JSON context")
    void testDataFileLoadsValidJson() {
        // Load data from JSON file without additional context
        assertDoesNotThrow(() -> {
            String result = templateService.render(
                "templates/test-simple.json.vm",
                "templates/test-data.json",
                (Map<String, Object>) null
            );
            assertNotNull(result);
            // Should use values from JSON file
            assertTrue(result.contains("Test Product"));
            assertTrue(result.contains("99"));
        });
    }

    @Test
    @DisplayName("Test data file with nested objects and arrays")
    void testDataFileWithNestedStructures() {
        String result = templateService.render(
            "templates/test-simple.json.vm",
            "templates/test-data.json"
        );

        assertNotNull(result);
        // Should successfully load and use data from JSON file
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    @DisplayName("Test non-existent data file throws exception")
    void testNonExistentDataFile() {
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render(
                "templates/test-simple.json.vm",
                "templates/non-existent-data.json",
                additionalContext
            );
        });
    }

    @Test
    @DisplayName("Test null data file path throws exception")
    void testNullDataFilePath() {
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render("templates/test-simple.json.vm", null, additionalContext);
        });
    }

    @Test
    @DisplayName("Test empty data file path throws exception")
    void testEmptyDataFilePath() {
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("name", "Test");

        assertThrows(TemplateException.class, () -> {
            templateService.render("templates/test-simple.json.vm", "", additionalContext);
        });
    }
}

