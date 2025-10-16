package com.sdg.logging;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

/**
 * Singleton class for loading and accessing environment variables from a .env file.
 * Loads the file once and provides global access to environment values.
 *
 * @author Joakim Colloz
 */
public final class EnvConfig {

    private static final EnvConfig INSTANCE = new EnvConfig();
    private final Dotenv dotenv;

    private EnvConfig() {
        LoggerUtil.info(getClass(), "Initializing EnvConfig and loading .env file.");
        Dotenv tempDotenv;
        try {
            tempDotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            LoggerUtil.info(getClass(), ".env file loaded successfully");
        } catch (DotenvException e) {
            LoggerUtil.error(getClass(), "Failed to load .env file: {}", e.getMessage(), e);
            tempDotenv = null;
        }
        this.dotenv = tempDotenv;
    }

    public static EnvConfig getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        if (dotenv == null) {
            LoggerUtil.warn(getClass(), "Dotenv not initialized; falling back to system environment.");
            return System.getenv(key);
        }

        String value = dotenv.get(key);
        if (value == null) {
            value = System.getenv(key);
            if (value == null) {
                LoggerUtil.warn(getClass(), "Environment variable not found: {}", key);
            }
        }
        return value;
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        if (value == null) {
            LoggerUtil.info(getClass(), "No value found for key: {}. Using default value", key);
            return defaultValue;
        }

        return value;
    }
}
