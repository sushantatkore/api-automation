package com.automation.api.core;

import com.automation.api.utils.JsonUtils;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Base class offering reusable HTTP verb helpers. All concrete services should extend this
 * so they never duplicate GET/POST/PUT/DELETE plumbing.
 */
public abstract class BaseService {

    protected final Logger log = LogManager.getLogger(getClass());
    private final RetryHandler retryHandler = RetryHandler.defaults();

    protected RequestSpecification request() {
        return RequestBuilder.create().build();
    }

    protected RequestSpecification unauthenticatedRequest() {
        return RequestBuilder.createUnauthenticated().build();
    }

    protected ApiResponse get(String path) {
        return get(path, Collections.emptyMap(), Collections.emptyMap());
    }

    protected ApiResponse get(String path, Map<String, ?> queryParams) {
        return get(path, queryParams, Collections.emptyMap());
    }

    protected ApiResponse get(String path, Map<String, ?> queryParams, Map<String, ?> pathParams) {
        return retryHandler.execute("GET " + path, () -> {
            log.info("GET {} query={} pathParams={}", path, queryParams, pathParams);
            return new ApiResponse(
                    request()
                         .queryParams(queryParams)
                            .pathParams(pathParams)
                            .get(path)
                            .then().extract().response());
        });
    }

    protected ApiResponse post(String path, Object body) {
        return post(path, body, Collections.emptyMap());
    }

    protected ApiResponse post(String path, Object body, Map<String, ?> pathParams) {
        return retryHandler.execute("POST " + path, () -> {
            String payload = body == null ? "" : JsonUtils.toJson(body);
            log.info("POST {} pathParams={} body={}", path, pathParams, payload);
            return new ApiResponse(
                    request()
                            .pathParams(pathParams)
                            .body(payload)
                            .post(path)
                            .then().extract().response());
        });
    }

    protected ApiResponse put(String path, Object body, Map<String, ?> pathParams) {
        return retryHandler.execute("PUT " + path, () -> {
            String payload = body == null ? "" : JsonUtils.toJson(body);
            log.info("PUT {} pathParams={} body={}", path, pathParams, payload);
            return new ApiResponse(
                    request()
                            .pathParams(pathParams)
                            .body(payload)
                            .put(path)
                            .then().extract().response());
        });
    }

    protected ApiResponse patch(String path, Object body, Map<String, ?> pathParams) {
        return retryHandler.execute("PATCH " + path, () -> {
            String payload = body == null ? "" : JsonUtils.toJson(body);
            log.info("PATCH {} pathParams={} body={}", path, pathParams, payload);
            return new ApiResponse(
                    request()
                            .pathParams(pathParams)
                            .body(payload)
                            .patch(path)
                            .then().extract().response());
        });
    }

    protected ApiResponse delete(String path, Map<String, ?> pathParams) {
        return retryHandler.execute("DELETE " + path, () -> {
            log.info("DELETE {} pathParams={}", path, pathParams);
            return new ApiResponse(
                    request()
                            .pathParams(pathParams)
                            .delete(path)
                            .then().extract().response());
        });
    }

    protected ApiResponse delete(String path) {
        return delete(path, Collections.emptyMap());
    }
}
