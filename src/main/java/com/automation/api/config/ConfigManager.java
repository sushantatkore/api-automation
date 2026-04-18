package com.automation.api.config;

import com.automation.api.exceptions.FrameworkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized, thread-safe configuration loader.
 * Resolves properties from: JVM system property -> OS env -> env-specific file -> defaults file.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    private static final String DEFAULT_CONFIG = "config.properties";
    private static final String ENV_PROPERTY = "env";
    private static final String DEFAULT_ENV = "qa";

    private static volatile ConfigManager instance;

    private final Properties properties = new Properties();
    private final String environment;

    private ConfigManager() {
        this.environment = resolveEnvironment();
        loadResource(DEFAULT_CONFIG);
        loadResource(String.format("config-%s.properties", environment));
        LOGGER.info("Configuration initialized for environment [{}]", environment);
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private String resolveEnvironment() {
        String env = System.getProperty(ENV_PROPERTY);
        if (env == null || env.isBlank()) {
            env = System.getenv("TEST_ENV");
        }
        return (env == null || env.isBlank()) ? DEFAULT_ENV : env.toLowerCase();
    }

    private void loadResource(String resource) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                LOGGER.warn("Config resource [{}] not found on classpath — skipping", resource);
                return;
            }
            Properties p = new Properties();
            p.load(stream);
            properties.putAll(p);
            LOGGER.debug("Loaded {} properties from [{}]", p.size(), resource);
        } catch (IOException e) {
            throw new FrameworkException("Failed to load configuration file: " + resource, e);
        }
    }

    public String get(String key) {
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new FrameworkException("Missing configuration key: " + key);
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (FrameworkException e) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        return get("api.base.url");
    }

    public String getAuthBaseUrl() {
        return get("auth.base.url", getBaseUrl());
    }

    public int getConnectionTimeout() {
        return getInt("api.connection.timeout.ms", 10_000);
    }

    public int getReadTimeout() {
        return getInt("api.read.timeout.ms", 30_000);
    }

    public int getRetryCount() {
        return getInt("api.retry.count", 2);
    }

    public long getRetryBackoffMs() {
        return getLong("api.retry.backoff.ms", 500L);
    }

    public boolean isLoggingEnabled() {
        return getBoolean("api.logging.enabled", true);
    }
}
