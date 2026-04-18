package com.automation.api.core;

import com.automation.api.auth.TokenManager;
import com.automation.api.config.ConfigManager;
import com.automation.api.constants.Headers;
import com.automation.api.utils.CorrelationIdUtil;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds Rest Assured RequestSpecifications with consistent base URL, timeouts,
 * auth, correlation ID, logging, and Allure attachment.
 */
public final class RequestBuilder {

    private static final Logger LOGGER = LogManager.getLogger(RequestBuilder.class);

    private final ConfigManager config = ConfigManager.getInstance();
    private final TokenManager tokenManager;
    private boolean requiresAuth = true;
    private String basePathOverride;

    private RequestBuilder(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public static RequestBuilder create() {
        return new RequestBuilder(TokenManager.getInstance());
    }

    public static RequestBuilder createUnauthenticated() {
        return new RequestBuilder(null).noAuth();
    }

    public RequestBuilder noAuth() {
        this.requiresAuth = false;
        return this;
    }

    public RequestBuilder withBaseUri(String baseUri) {
        this.basePathOverride = baseUri;
        return this;
    }

    public RequestSpecification build() {
        String correlationId = CorrelationIdUtil.current();
        String baseUri = basePathOverride != null ? basePathOverride : config.getBaseUrl();

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader(Headers.CORRELATION_ID, correlationId)
                .addHeader(Headers.REQUEST_ID, correlationId)
                .addHeader(Headers.USER_AGENT, "api-automation-framework/1.0")
                .setConfig(buildRestAssuredConfig());

        if (requiresAuth && tokenManager != null) {
            String token = tokenManager.getToken();
            builder.addHeader(Headers.AUTHORIZATION, Headers.BEARER_PREFIX + token);
        }

        RequestSpecification spec = builder.build();

        if (config.isLoggingEnabled()) {
            spec.filter(new RequestLoggingFilter());
            spec.filter(new ResponseLoggingFilter());
        }
        spec.filter(new AllureRestAssured());

        LOGGER.debug("Built request spec baseUri={} auth={} correlationId={}", baseUri, requiresAuth, correlationId);
        return RestAssured.given().spec(spec);
    }

    private RestAssuredConfig buildRestAssuredConfig() {
        return RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", config.getConnectionTimeout())
                    .setParam("http.socket.timeout", config.getReadTimeout())
                    .setParam("http.connection-manager.timeout", (long) config.getConnectionTimeout()));
    }
}
