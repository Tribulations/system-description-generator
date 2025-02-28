package com.sdg.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for logging that wraps SLF4J functionality.
 * Provides static methods for logging at different levels without requiring
 * client classes to directly import SLF4J classes.
 */
public class LoggerUtil {
    private static Logger getLoggerInstance(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void info(Class<?> clazz, String message) {
        getLoggerInstance(clazz).info(message);
    }

    public static void info(Class<?> clazz, String format, Object... args) {
        getLoggerInstance(clazz).info(format, args);
    }

    public static void debug(Class<?> clazz, String message) {
        getLoggerInstance(clazz).debug(message);
    }

    public static void debug(Class<?> clazz, String format, Object... args) {
        getLoggerInstance(clazz).debug(format, args);
    }

    public static void error(Class<?> clazz, String message) {
        getLoggerInstance(clazz).error(message);
    }

    public static void error(Class<?> clazz, String format, Object... args) {
        getLoggerInstance(clazz).error(format, args);
    }

    public static void warn(Class<?> clazz, String message) {
        getLoggerInstance(clazz).warn(message);
    }

    public static void warn(Class<?> clazz, String format, Object... args) {
        getLoggerInstance(clazz).warn(format, args);
    }
}
