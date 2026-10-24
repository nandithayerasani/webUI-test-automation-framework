package utils;

import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Reads settings in three layers, each one overriding the one before:
 *   1. config.properties                       - shared settings (browser, timeouts, ...)
 *   2. environments/&lt;env&gt;.properties          - per-environment settings (URL, data sheet, ...)
 *   3. JVM system properties (-Dkey=value)     - per-run overrides
 *
 * The environment comes from "env" (default qa), e.g. mvn test -Denv=uat
 */
public final class ConfigReader {

    private static final Logger LOG = LoggerUtil.getLogger(ConfigReader.class);
    private static final Properties PROPERTIES = load();

    private ConfigReader() {
    }

    private static Properties load() {
        Properties props = new Properties();
        loadInto(props, FrameworkConstants.CONFIG_FILE);

        String env = System.getProperty("env", props.getProperty("env", "qa")).trim().toLowerCase(Locale.ROOT);
        props.setProperty("env", env);
        loadInto(props, FrameworkConstants.ENVIRONMENTS_DIR + "/" + env + ".properties");
        LOG.info("Environment: {}", env);
        return props;
    }

    private static void loadInto(Properties props, String resource) {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " not found on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + resource, e);
        }
    }

    /** The environment this run targets: qa, sit, uat, ... */
    public static String getEnv() {
        return get("env");
    }

    /** Returns the value for key, or throws if it is missing everywhere. */
    public static String get(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing config value: " + key);
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        String value = PROPERTIES.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Secret from an environment variable, falling back to a config key / -D property.
     * Throws with a clear message when neither is set.
     */
    public static String getSecret(String envVar, String configKey) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            value = get(configKey, null);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set the " + envVar + " environment variable");
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
