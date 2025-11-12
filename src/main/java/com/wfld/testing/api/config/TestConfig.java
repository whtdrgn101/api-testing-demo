package com.wfld.testing.api.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for loading test properties
 */
@Slf4j
@Getter
public class TestConfig {
    private static final String CONFIG_FILE = "application.properties";
    private static TestConfig instance;
    private final Properties properties;

    private TestConfig() {
        properties = new Properties();
        loadProperties();
    }

    public static synchronized TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                log.warn("Configuration file {} not found, using system properties", CONFIG_FILE);
                return;
            }
            properties.load(inputStream);
            log.info("Configuration loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Error loading configuration file", e);
        }
    }

    public String getProperty(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public String getPingFederateBaseUrl() {
        return getProperty("ping.federate.base.url");
    }

    public String getPingFederateTokenEndpoint() {
        return getProperty("ping.federate.token.endpoint", "/as/token.oauth2");
    }

    public String getPingFederateClientId() {
        return getProperty("ping.federate.client.id");
    }

    public String getPingFederateClientSecret() {
        return getProperty("ping.federate.client.secret");
    }

    public String getPingFederateGrantType() {
        return getProperty("ping.federate.grant.type", "client_credentials");
    }

    public String getApiBaseUrl() {
        return getProperty("api.base.url");
    }

    public int getTestTimeout() {
        return Integer.parseInt(getProperty("test.timeout", "30000"));
    }

    public int getTestConnectionTimeout() {
        return Integer.parseInt(getProperty("test.connection.timeout", "10000"));
    }
}

