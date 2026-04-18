package com.automation.api.utils;

import org.apache.logging.log4j.ThreadContext;

import java.util.UUID;

/**
 * Per-thread correlation ID. Stored in Log4j ThreadContext so all log lines
 * for a given request/test share the same ID.
 */
public final class CorrelationIdUtil {

    public static final String CORRELATION_KEY = "correlationId";

    private CorrelationIdUtil() { }

    public static String generate() {
        String id = UUID.randomUUID().toString();
        ThreadContext.put(CORRELATION_KEY, id);
        return id;
    }

    public static String current() {
        String existing = ThreadContext.get(CORRELATION_KEY);
        return existing != null ? existing : generate();
    }

    public static void set(String id) {
        ThreadContext.put(CORRELATION_KEY, id);
    }

    public static void clear() {
        ThreadContext.remove(CORRELATION_KEY);
    }
}
