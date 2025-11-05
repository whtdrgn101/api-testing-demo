package com.insurance.api.testing.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify OAuth scopes for test classes.
 * When applied to a test class, the specified scopes will be included in the token request.
 * 
 * Example usage:
 * <pre>
 * {@code @OAuthScopes({"read", "write"})
 * public class MyApiTest extends BaseApiTest {
 *     // test methods
 * }
 * </pre>
 * 
 * If not specified, the token request will be made without scopes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OAuthScopes {
    /**
     * Array of OAuth scope strings to include in the token request
     * 
     * @return array of scope strings
     */
    String[] value() default {};
}

