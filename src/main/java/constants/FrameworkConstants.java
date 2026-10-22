package constants;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central place for fixed values used across the framework.
 * Anything an engineer may want to change per run belongs in config.properties instead.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {
    }

    private static final Path PROJECT_DIR = Paths.get(System.getProperty("user.dir"));

    public static final String CONFIG_FILE = "config.properties";
    public static final String BROWSERSTACK_CONFIG_FILE = "browserstack.json";

    public static final Path TEST_DATA_DIR = PROJECT_DIR.resolve("src/test/resources/testdata");
    public static final Path REPORTS_DIR = PROJECT_DIR.resolve("reports");
    public static final Path SCREENSHOTS_DIR = REPORTS_DIR.resolve("screenshots");
    public static final String EXTENT_REPORT_NAME = "ExtentReport.html";

    public static final int DEFAULT_EXPLICIT_WAIT_SECONDS = 10;
    public static final int DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS = 30;
}
