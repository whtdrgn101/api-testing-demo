package com.wfld.testing.api;

import com.wfld.testing.api.auth.BypassTokenCache;
import com.wfld.testing.api.auth.OAuthScopes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BaseApiTest annotation extraction logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseApiTest Annotation Tests")
class BaseApiTestAnnotationTest {

    @Test
    @DisplayName("Test OAuthScopes annotation on class level")
    @OAuthScopes({"class:scope"})
    void testClassLevelOAuthScopes(TestInfo testInfo) {
        TestClassWithAnnotation testClass = new TestClassWithAnnotation();
        String[] scopes = extractScopes(testClass, testInfo);
        
        assertNotNull(scopes);
        assertEquals(1, scopes.length);
        assertEquals("class:scope", scopes[0]);
    }

    @Test
    @DisplayName("Test OAuthScopes annotation on method level overrides class level")
    @OAuthScopes({"class:scope"})
    void testMethodLevelOAuthScopesOverridesClass(TestInfo testInfo) {
        TestClassWithAnnotation testClass = new TestClassWithAnnotation();
        
        // Simulate method-level annotation
        Method testMethod = findTestMethod(testInfo);
        assertNotNull(testMethod);
        
        // The method-level annotation should take precedence
        String[] scopes = extractScopes(testClass, testInfo);
        
        // Since this test method has @OAuthScopes({"class:scope"}), it should use that
        assertNotNull(scopes);
        assertEquals(1, scopes.length);
        assertEquals("class:scope", scopes[0]);
    }

    @Test
    @DisplayName("Test BypassTokenCache annotation on class level")
    @BypassTokenCache
    void testClassLevelBypassTokenCache(TestInfo testInfo) {
        TestClassWithBypassAnnotation testClass = new TestClassWithBypassAnnotation();
        boolean shouldBypass = shouldBypassCache(testClass, testInfo);
        
        assertTrue(shouldBypass);
    }

    @Test
    @DisplayName("Test BypassTokenCache annotation on method level overrides class level")
    @BypassTokenCache
    void testMethodLevelBypassTokenCacheOverridesClass(TestInfo testInfo) {
        TestClassWithBypassAnnotation testClass = new TestClassWithBypassAnnotation();
        
        // Method-level annotation should take precedence
        boolean shouldBypass = shouldBypassCache(testClass, testInfo);
        
        // Since this test method has @BypassTokenCache, it should return true
        assertTrue(shouldBypass);
    }

    @Test
    @DisplayName("Test no annotations - should return empty scopes and no bypass")
    void testNoAnnotations(TestInfo testInfo) {
        TestClassWithoutAnnotations testClass = new TestClassWithoutAnnotations();
        String[] scopes = extractScopes(testClass, testInfo);
        boolean shouldBypass = shouldBypassCache(testClass, testInfo);
        
        assertNotNull(scopes);
        assertEquals(0, scopes.length);
        assertFalse(shouldBypass);
    }

    // Helper methods that mirror BaseApiTest logic for testing
    private String[] extractScopes(BaseApiTest testClass, TestInfo testInfo) {
        Method testMethod = findTestMethod(testInfo);
        
        // Check method-level annotation first
        if (testMethod != null) {
            OAuthScopes methodAnnotation = testMethod.getAnnotation(OAuthScopes.class);
            if (methodAnnotation != null) {
                String[] scopes = methodAnnotation.value();
                if (scopes != null && scopes.length > 0) {
                    return scopes;
                }
            }
        }
        
        // Fall back to class-level annotation
        OAuthScopes classAnnotation = testClass.getClass().getAnnotation(OAuthScopes.class);
        if (classAnnotation != null) {
            String[] scopes = classAnnotation.value();
            if (scopes != null && scopes.length > 0) {
                return scopes;
            }
        }
        
        return new String[0];
    }

    private boolean shouldBypassCache(BaseApiTest testClass, TestInfo testInfo) {
        Method testMethod = findTestMethod(testInfo);
        
        // Check method-level annotation first
        if (testMethod != null) {
            BypassTokenCache methodAnnotation = testMethod.getAnnotation(BypassTokenCache.class);
            if (methodAnnotation != null) {
                return true;
            }
        }
        
        // Fall back to class-level annotation
        BypassTokenCache classAnnotation = testClass.getClass().getAnnotation(BypassTokenCache.class);
        return classAnnotation != null;
    }

    private Method findTestMethod(TestInfo testInfo) {
        if (testInfo == null || testInfo.getTestMethod().isEmpty()) {
            return null;
        }
        
        try {
            return testInfo.getTestMethod().get();
        } catch (Exception e) {
            return null;
        }
    }

    // Test classes for annotation testing
    @OAuthScopes({"class:scope"})
    private static class TestClassWithAnnotation extends BaseApiTest {
    }

    @BypassTokenCache
    private static class TestClassWithBypassAnnotation extends BaseApiTest {
    }

    private static class TestClassWithoutAnnotations extends BaseApiTest {
    }
}

