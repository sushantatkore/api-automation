package com.automation.api.auth;

import com.automation.api.config.ConfigManager;
import com.automation.api.constants.Endpoints;
import com.automation.api.exceptions.FrameworkException;
import com.automation.api.models.AuthRequest;
import com.automation.api.models.AuthResponse;
import com.automation.api.utils.JsonUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe token cache with expiry-aware refresh.
 * Credentials are sourced only from config (never hardcoded).
 */
public final class TokenManager {

    private static final Logger LOGGER = LogManager.getLogger(TokenManager.class);
    private static final long EXPIRY_SKEW_SECONDS = 30L;

    private static volatile TokenManager instance;

    private final ConfigManager config = ConfigManager.getInstance();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant cachedExpiry;

    private TokenManager() { }

    public static TokenManager getInstance() {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager();
                }
            }
        }
        return instance;
    }

    public String getToken() {
        if (isCachedTokenValid()) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (isCachedTokenValid()) {
                return cachedToken;
            }
            refreshToken();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    public void invalidate() {
        lock.lock();
        try {
            cachedToken = null;
            cachedExpiry = null;
            LOGGER.debug("Token cache invalidated");
        } finally {
            lock.unlock();
        }
    }

    private boolean isCachedTokenValid() {
        return cachedToken != null
                && cachedExpiry != null
                && Instant.now().isBefore(cachedExpiry.minusSeconds(EXPIRY_SKEW_SECONDS));
    }

    private void refreshToken() {
        String staticToken = config.get("auth.static.token", "");
        if (!staticToken.isBlank()) {
            cachedToken = staticToken;
            cachedExpiry = Instant.now().plusSeconds(config.getLong("auth.static.ttl.seconds", 3600L));
            LOGGER.info("Using static token from configuration (ttl seconds={})", config.getLong("auth.static.ttl.seconds", 3600L));
            return;
        }

        AuthRequest request = AuthRequest.builder()
                .username(config.get("auth.username", ""))
                .password(config.get("auth.password", ""))
                .clientId(config.get("auth.client.id", ""))
                .clientSecret(config.get("auth.client.secret", ""))
                .grantType(config.get("auth.grant.type", "password"))
                .scope(config.get("auth.scope", ""))
                .build();

        String authUrl = config.getAuthBaseUrl() + Endpoints.Auth.LOGIN;
        LOGGER.info("Requesting new auth token from {}", authUrl);

        Response response;
        try {
            response = RestAssured.given()
                    .baseUri(config.getAuthBaseUrl())
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(JsonUtils.toJson(request))
                    .post(Endpoints.Auth.LOGIN)
                    .then().extract().response();
        } catch (Exception e) {
            throw new FrameworkException("Failed to call auth endpoint: " + authUrl, e);
        }

        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new FrameworkException("Auth call failed with status " + response.getStatusCode()
                    + " body=" + response.getBody().asString());
        }

        AuthResponse parsed = JsonUtils.fromJson(response.getBody().asString(), AuthResponse.class);
        if (parsed.getAccessToken() == null || parsed.getAccessToken().isBlank()) {
            throw new FrameworkException("Auth response did not include an access token: "
                    + response.getBody().asString());
        }

        long ttl = parsed.getExpiresInSeconds() != null ? parsed.getExpiresInSeconds()
                : config.getLong("auth.token.ttl.seconds", 3600L);
        cachedToken = parsed.getAccessToken();
        cachedExpiry = Instant.now().plusSeconds(ttl);
        LOGGER.info("Auth token acquired (expires in {}s)", ttl);
    }
}
