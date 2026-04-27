package com.automation.api.utils;

import com.automation.api.core.ApiResponse;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Objects;

/**
 * Reusable, descriptive assertions that also emit log + Allure breadcrumbs.
 */
public final class AssertionUtils {

    private static final Logger LOGGER = LogManager.getLogger(AssertionUtils.class);

    private AssertionUtils() { }

    public static void assertStatusCode(ApiResponse response, int expected) {
        int actual = response.statusCode();
        step("Assert status code " + actual + " == " + expected);
        if (actual != expected) {
            attachBody(response);
            Assert.fail(String.format("Expected status code %d but got %d. Body: %s",
                    expected, actual, response.body()));
        }
    }

    public static void assertStatusCodeIn(ApiResponse response, int... expected) {
        int actual = response.statusCode();
        for (int code : expected) {
            if (code == actual) {
                step("Assert status code " + actual + " in expected set");
                return;
            }
        }
        attachBody(response);
        Assert.fail("Unexpected status code " + actual + ". Body: " + response.body());
    }

    public static void assertResponseTimeUnder(ApiResponse response, long maxMillis) {
        long actual = response.responseTimeMs();
        step(String.format("Assert response time %dms <= %dms", actual, maxMillis));
        if (actual > maxMillis) {
            Assert.fail(String.format("Response time %dms exceeded max %dms", actual, maxMillis));
        }
    }

    public static void assertEquals(Object actual, Object expected, String fieldName) {
        step(String.format("Assert %s equals expected", fieldName));
        Assert.assertEquals(actual, expected, "Field [" + fieldName + "] mismatch");
    }

    public static void assertNotBlank(String value, String fieldName) {
        step("Assert " + fieldName + " is not blank");
        if (value == null || value.isBlank()) {
            Assert.fail("Field [" + fieldName + "] is null or blank");
        }
    }

    public static void assertTrue(boolean condition, String message) {
        step("Assert: " + message);
        Assert.assertTrue(condition, message);
    }

    public static void assertContainsHeader(ApiResponse response, String headerName) {
        step("Assert response contains header " + headerName);
        Assert.assertNotNull(response.header(headerName),
                "Expected header [" + headerName + "] but it was missing");
    }

    public static void assertJsonPathEquals(ApiResponse response, String jsonPath, Object expected) {
        Object actual = response.jsonPath(jsonPath);
        step(String.format("Assert JSON path %s = %s", jsonPath, expected));
        if (!Objects.equals(actual, expected)) {
            attachBody(response);
            Assert.fail(String.format("JSON path [%s] expected=%s actual=%s", jsonPath, expected, actual));
        }
    }

    private static void step(String message) {
        LOGGER.info("ASSERT: {}", message);
        Allure.step(message);
    }

    private static void attachBody(ApiResponse response) {
        try {
            Allure.addAttachment("Failed Response Body", "application/json", response.body(), ".json");
        } catch (Exception ignore) { }
    }
}
