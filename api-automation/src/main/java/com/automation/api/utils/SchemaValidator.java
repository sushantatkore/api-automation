package com.automation.api.utils;

import com.automation.api.core.ApiResponse;
import com.automation.api.exceptions.FrameworkException;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thin wrapper over Rest Assured's JSON schema validator. Schema path is resolved
 * from the classpath (e.g. "schemas/user-schema.json").
 */
public final class SchemaValidator {

    private static final Logger LOGGER = LogManager.getLogger(SchemaValidator.class);

    private SchemaValidator() { }

    public static void validate(ApiResponse response, String schemaClasspath) {
        LOGGER.debug("Validating response against schema [{}]", schemaClasspath);
        try {
            response.raw().then().assertThat()
                    .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaClasspath));
        } catch (AssertionError e) {
            throw new AssertionError("Schema validation failed against " + schemaClasspath + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FrameworkException("Unable to run schema validation for " + schemaClasspath, e);
        }
    }
}
