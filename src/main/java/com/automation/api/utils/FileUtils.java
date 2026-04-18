package com.automation.api.utils;

import com.automation.api.exceptions.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class FileUtils {

    private FileUtils() { }

    public static String readClasspathResource(String resourcePath) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new FrameworkException("Resource not found on classpath: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FrameworkException("Failed to read classpath resource: " + resourcePath, e);
        }
    }

    public static InputStream openClasspathStream(String resourcePath) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new FrameworkException("Resource not found on classpath: " + resourcePath);
        }
        return stream;
    }
}
