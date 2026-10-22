package utils;

import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads values from config.properties.
 * A JVM system property with the same key wins, so any value can be overridden per run:
 *   mvn test -Dbrowser=edge -Dprivate.mode=true
 */
public final class ConfigReader {

    private static final Logger LOG = LoggerUtil.getLogger(ConfigReader.class);
    private static final Properties PROPERTIES = load();

    private ConfigReader() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream(FrameworkConstants.CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(FrameworkConstants.CONFIG_FILE + " not found on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FrameworkConstants.CONFIG_FILE, e);
        }
        LOG.info("Loaded {}", FrameworkConstants.CONFIG_FILE);
        return props;
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

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
