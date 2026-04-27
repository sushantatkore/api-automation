package com.automation.api.utils;

import com.automation.api.core.ApiResponse;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logs and attaches request/response payloads to Allure.
 * RequestBuilder already applies AllureRestAssured + logging filters; this helper
 * is for ad-hoc log points inside workflows.
 */
public final class LogUtils {

    private static final Logger LOGGER = LogManager.getLogger(LogUtils.class);

    private LogUtils() { }

    public static void attachResponse(String name, ApiResponse response) {
        String body = response.body();
        LOGGER.info("{} status={} timeMs={}", name, response.statusCode(), response.responseTimeMs());
        Allure.addAttachment(name, "application/json", body, ".json");
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
        Allure.step(String.format(message.replace("{}", "%s"), args));
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }
}
