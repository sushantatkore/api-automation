package com.automation.api.core;

import com.automation.api.utils.JsonUtils;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

/**
 * Lightweight wrapper around Rest Assured Response. Services expose this type
 * so tests never touch Rest Assured directly.
 */
public class ApiResponse {

    private final Response response;

    public ApiResponse(Response response) {
        this.response = response;
    }

    public Response raw() {
        return response;
    }

    public int statusCode() {
        return response.getStatusCode();
    }

    public String body() {
        return response.getBody().asString();
    }

    public long responseTimeMs() {
        return response.getTime();
    }

    public String header(String name) {
        return response.getHeader(name);
    }

    public Map<String, String> headers() {
        return response.getHeaders().asList().stream()
                .collect(java.util.stream.Collectors.toMap(
                        io.restassured.http.Header::getName,
                        io.restassured.http.Header::getValue,
                        (a, b) -> a));
    }

    public <T> T as(Class<T> type) {
        return JsonUtils.fromJson(body(), type);
    }

    public <T> List<T> asList(Class<T> elementType) {
        return JsonUtils.fromJsonToList(body(), elementType);
    }

    public <T> T jsonPath(String path) {
        return response.jsonPath().get(path);
    }

    public boolean isSuccess() {
        int code = statusCode();
        return code >= 200 && code < 300;
    }

    public boolean isServerError() {
        int code = statusCode();
        return code >= 500 && code < 600;
    }
}
