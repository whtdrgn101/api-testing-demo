package com.westfieldgrp.testing.api.playwright;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Predicate;

/**
 * Matcher functions similar to REST-assured's Hamcrest matchers.
 * Provides common validation predicates for use with PlaywrightApiRequest.
 */
public class PlaywrightMatchers {

    /**
     * Creates a matcher that checks if a value equals the expected value
     */
    public static Predicate<JsonNode> equalTo(Object expected) {
        return node -> {
            Object actual = getNodeValue(node);
            if (expected == null) {
                return actual == null;
            }
            return expected.equals(actual) || expected.toString().equals(actual.toString());
        };
    }

    /**
     * Creates a matcher that checks if a value is not null
     */
    public static Predicate<JsonNode> notNullValue() {
        return node -> node != null && !node.isNull();
    }

    /**
     * Creates a matcher that checks if a value is null
     */
    public static Predicate<JsonNode> nullValue() {
        return node -> node == null || node.isNull();
    }

    /**
     * Creates a matcher that checks if a string contains the expected substring
     */
    public static Predicate<JsonNode> containsString(String substring) {
        return node -> {
            String value = node.asText();
            return value != null && value.contains(substring);
        };
    }

    /**
     * Creates a matcher that checks if a number is greater than the expected value
     */
    public static Predicate<JsonNode> greaterThan(Number expected) {
        return node -> {
            if (node.isNumber()) {
                return node.asDouble() > expected.doubleValue();
            }
            return false;
        };
    }

    /**
     * Creates a matcher that checks if a number is less than the expected value
     */
    public static Predicate<JsonNode> lessThan(Number expected) {
        return node -> {
            if (node.isNumber()) {
                return node.asDouble() < expected.doubleValue();
            }
            return false;
        };
    }

    /**
     * Creates a matcher that checks if an array has the expected size
     */
    public static Predicate<JsonNode> hasSize(int expectedSize) {
        return node -> {
            if (node.isArray()) {
                return node.size() == expectedSize;
            }
            return false;
        };
    }

    /**
     * Creates a matcher that checks if an array contains the expected item
     */
    public static Predicate<JsonNode> hasItem(Object expected) {
        return node -> {
            if (node.isArray()) {
                for (JsonNode item : node) {
                    Object value = getNodeValue(item);
                    if (expected.equals(value) || expected.toString().equals(value.toString())) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    private static Object getNodeValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else {
                return node.asDouble();
            }
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        } else {
            return node.toString();
        }
    }
}

