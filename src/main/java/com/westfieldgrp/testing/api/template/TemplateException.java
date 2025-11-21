package com.westfieldgrp.testing.api.template;

/**
 * Exception thrown when template processing fails
 */
public class TemplateException extends RuntimeException {
    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}

