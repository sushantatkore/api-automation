package com.automation.api.exceptions;

/**
 * Unified runtime exception for framework-level failures
 * (config loading, token generation, schema parsing, etc.).
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
