package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thin wrapper around Log4j2 so classes don't depend on the logging library directly.
 * Logging configuration lives in src/test/resources/log4j2.xml.
 */
public final class LoggerUtil {

    private LoggerUtil() {
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
}
