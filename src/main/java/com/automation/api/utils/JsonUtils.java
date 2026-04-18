package com.automation.api.utils;

import com.automation.api.exceptions.FrameworkException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() { }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new FrameworkException("Failed to serialize object to JSON", e);
        }
    }

    public static String toPrettyJson(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (IOException e) {
            throw new FrameworkException("Failed to serialize object to pretty JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new FrameworkException("Failed to deserialize JSON to " + type.getName(), e);
        }
    }

    public static <T> List<T> fromJsonToList(String json, Class<T> elementType) {
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new FrameworkException("Failed to deserialize JSON list of " + elementType.getName(), e);
        }
    }

    public static Map<String, Object> fromJsonToMap(String json) {
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (IOException e) {
            throw new FrameworkException("Failed to deserialize JSON to Map", e);
        }
    }

    public static <T> T readResource(String resourcePath, Class<T> type) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new FrameworkException("Resource not found: " + resourcePath);
            }
            return MAPPER.readValue(stream, type);
        } catch (IOException e) {
            throw new FrameworkException("Failed to read resource: " + resourcePath, e);
        }
    }

    public static <T> List<T> readResourceAsList(String resourcePath, Class<T> elementType) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new FrameworkException("Resource not found: " + resourcePath);
            }
            return MAPPER.readValue(stream,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new FrameworkException("Failed to read resource list: " + resourcePath, e);
        }
    }

    public static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new FrameworkException("Failed to read file: " + path, e);
        }
    }
}
