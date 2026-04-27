package com.automation.api.core;

import com.automation.api.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Retries a {@link ApiResponse}-producing call on 5xx responses or exceptions
 * up to a configurable count with exponential backoff.
 */
public final class RetryHandler {

    private static final Logger LOGGER = LogManager.getLogger(RetryHandler.class);

    private final int maxAttempts;
    private final long backoffMs;

    private RetryHandler(int maxAttempts, long backoffMs) {
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    public static RetryHandler defaults() {
        ConfigManager cfg = ConfigManager.getInstance();
        return new RetryHandler(Math.max(1, cfg.getRetryCount() + 1), cfg.getRetryBackoffMs());
    }

    public static RetryHandler of(int maxAttempts, long backoffMs) {
        return new RetryHandler(Math.max(1, maxAttempts), backoffMs);
    }

    public ApiResponse execute(String description, Supplier<ApiResponse> call) {
        RuntimeException lastError = null;
        ApiResponse lastResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOGGER.debug("Executing [{}] attempt {}/{}", description, attempt, maxAttempts);
                ApiResponse response = call.get();
                if (!response.isServerError()) {
                    return response;
                }
                lastResponse = response;
                LOGGER.warn("[{}] attempt {}/{} returned {}. Retrying...",
                        description, attempt, maxAttempts, response.statusCode());
            } catch (RuntimeException e) {
                lastError = e;
                LOGGER.warn("[{}] attempt {}/{} threw {}: {}",
                        description, attempt, maxAttempts, e.getClass().getSimpleName(), e.getMessage());
            }

            if (attempt < maxAttempts) {
                sleep(backoffMs * (long) Math.pow(2, attempt - 1));
            }
        }

        if (lastResponse != null) {
            return lastResponse;
        }
        throw lastError != null ? lastError
                : new IllegalStateException("Retry exhausted without response or error for " + description);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
