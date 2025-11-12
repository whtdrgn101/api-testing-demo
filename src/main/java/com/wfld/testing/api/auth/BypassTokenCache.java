package com.wfld.testing.api.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bypass token cache for individual test methods.
 * When applied to a test method, it will force a fresh token to be retrieved,
 * even if a valid cached token exists for the same scopes.
 * 
 * Example usage:
 * <pre>
 * {@code @Test}
 * {@code @BypassTokenCache}
 * {@code @OAuthScopes({"read", "write"})}
 * public void testWithFreshToken() {
 *     // This test will always fetch a new token, bypassing the cache
 * }
 * </pre>
 * 
 * This is useful for testing token refresh scenarios or when you need to ensure
 * a fresh token is used for each test execution.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BypassTokenCache {
}

