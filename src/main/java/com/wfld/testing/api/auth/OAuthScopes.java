package com.wfld.testing.api.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify OAuth scopes for test classes or individual test methods.
 * When applied to a test class, the specified scopes will be included in the token request for all tests.
 * When applied to a test method, it will override the class-level scopes for that specific test.
 * 
 * Example usage:
 * <pre>
 * {@code @OAuthScopes({"read", "write"})
 * public class MyApiTest extends BaseApiTest {
 *     {@code @Test}
 *     {@code @OAuthScopes({"admin"})}
 *     public void testAdminEndpoint() {
 *         // This test will use "admin" scope only
 *     }
 *     
 *     {@code @Test}
 *     public void testRegularEndpoint() {
 *         // This test will use "read", "write" scopes from class level
 *     }
 * }
 * </pre>
 * 
 * If not specified, the token request will be made without scopes.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OAuthScopes {
    /**
     * Array of OAuth scope strings to include in the token request
     * 
     * @return array of scope strings
     */
    String[] value() default {};
}

