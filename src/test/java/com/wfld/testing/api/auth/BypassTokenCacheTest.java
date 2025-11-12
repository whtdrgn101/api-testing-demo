package com.wfld.testing.api.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BypassTokenCache annotation
 */
@DisplayName("BypassTokenCache Annotation Tests")
class BypassTokenCacheTest {

    @Test
    @DisplayName("Test BypassTokenCache annotation can be applied to class")
    void testAnnotationOnClass() {
        BypassTokenCache annotation = TestClassWithBypassTokenCache.class.getAnnotation(BypassTokenCache.class);
        
        assertNotNull(annotation, "Annotation should be present on class");
    }

    @Test
    @DisplayName("Test BypassTokenCache annotation can be applied to method")
    void testAnnotationOnMethod() throws NoSuchMethodException {
        java.lang.reflect.Method method = TestClassWithBypassTokenCache.class.getMethod("testMethod");
        BypassTokenCache annotation = method.getAnnotation(BypassTokenCache.class);
        
        assertNotNull(annotation, "Annotation should be present on method");
    }

    @Test
    @DisplayName("Test BypassTokenCache annotation retention policy")
    void testAnnotationRetention() {
        Annotation annotation = TestClassWithBypassTokenCache.class.getAnnotation(BypassTokenCache.class);
        
        assertNotNull(annotation);
        assertEquals(BypassTokenCache.class, annotation.annotationType());
    }

    @Test
    @DisplayName("Test class without BypassTokenCache annotation")
    void testClassWithoutAnnotation() {
        BypassTokenCache annotation = TestClassWithoutBypassTokenCache.class.getAnnotation(BypassTokenCache.class);
        
        assertNull(annotation, "Annotation should not be present on class without annotation");
    }

    @BypassTokenCache
    private static class TestClassWithBypassTokenCache {
        @BypassTokenCache
        public void testMethod() {
        }
    }

    private static class TestClassWithoutBypassTokenCache {
    }
}

