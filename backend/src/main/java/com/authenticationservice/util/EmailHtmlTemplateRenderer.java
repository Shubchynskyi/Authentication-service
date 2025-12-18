package com.authenticationservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Loads HTML templates from classpath and replaces simple placeholders like {{key}}.
 * This keeps email HTML in resource files instead of Java string constants.
 */
public final class EmailHtmlTemplateRenderer {

    private EmailHtmlTemplateRenderer() {}

    public static String renderFromClasspath(String classpathLocation, Map<String, String> variables) {
        String template = loadFromClasspath(classpathLocation);
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "template variable key must not be null");
            String value = entry.getValue() != null ? entry.getValue() : "";
            rendered = rendered.replace("{{" + key + "}}", value);
        }
        return rendered;
    }

    private static String loadFromClasspath(String classpathLocation) {
        String location = Objects.requireNonNull(classpathLocation, "classpathLocation must not be null");
        ClassLoader cl = EmailHtmlTemplateRenderer.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(location)) {
            if (in == null) {
                throw new IllegalStateException("Email template not found on classpath: " + location);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template: " + location, e);
        }
    }
}


