package com.westfieldgrp.testing.api.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OAuthScopes annotation
 */
@DisplayName("OAuthScopes Annotation Tests")
class OAuthScopesTest {

    @Test
    @DisplayName("Test OAuthScopes annotation can be applied to class")
    void testAnnotationOnClass() {
        OAuthScopes annotation = TestClassWithOAuthScopes.class.getAnnotation(OAuthScopes.class);
        
        assertNotNull(annotation, "Annotation should be present on class");
        assertArrayEquals(new String[]{"read", "write"}, annotation.value());
    }

    @Test
    @DisplayName("Test OAuthScopes annotation can be applied to method")
    void testAnnotationOnMethod() throws NoSuchMethodException {
        java.lang.reflect.Method method = TestClassWithOAuthScopes.class.getMethod("testMethod");
        OAuthScopes annotation = method.getAnnotation(OAuthScopes.class);
        
        assertNotNull(annotation, "Annotation should be present on method");
        assertArrayEquals(new String[]{"admin"}, annotation.value());
    }

    @Test
    @DisplayName("Test OAuthScopes annotation with empty scopes")
    void testAnnotationWithEmptyScopes() {
        OAuthScopes annotation = TestClassWithEmptyOAuthScopes.class.getAnnotation(OAuthScopes.class);
        
        assertNotNull(annotation);
        assertEquals(0, annotation.value().length);
    }

    @Test
    @DisplayName("Test OAuthScopes annotation retention policy")
    void testAnnotationRetention() {
        Annotation annotation = TestClassWithOAuthScopes.class.getAnnotation(OAuthScopes.class);
        
        assertNotNull(annotation);
        assertEquals(OAuthScopes.class, annotation.annotationType());
    }

    @OAuthScopes({"read", "write"})
    private static class TestClassWithOAuthScopes {
        @OAuthScopes({"admin"})
        public void testMethod() {
        }
    }

    @OAuthScopes({})
    private static class TestClassWithEmptyOAuthScopes {
    }
}

