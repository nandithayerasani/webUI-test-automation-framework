package base;

import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import utils.ConfigReader;
import utils.LoggerUtil;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates and holds one WebDriver per thread, so tests can run in parallel safely.
 * Selenium Manager (built into Selenium 4) downloads the matching browser driver automatically.
 *
 * Private browsing:
 *   Chrome  -> --incognito
 *   Edge    -> -inprivate
 *   Firefox -> -private
 * Controlled by "private.mode" in config.properties, a per-browser key such as
 * "edge.private.mode", the "privateMode" TestNG parameter, or -Dprivate.mode=true.
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerUtil.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    public enum Browser {
        CHROME, EDGE, FIREFOX;

        public static Browser from(String name) {
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("Unsupported browser: " + name
                        + ". Use one of " + Arrays.toString(values()), e);
            }
        }

        /** Lower-case name used as a prefix for browser-specific config keys. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private DriverFactory() {
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver has not been started on this thread. Call initDriver first.");
        }
        return driver;
    }

    /**
     * Starts a browser for the current thread.
     *
     * @param browserName chrome, edge or firefox; null uses "browser" from config
     * @param privateMode true/false to force private browsing; null uses config
     */
    public static WebDriver initDriver(String browserName, Boolean privateMode) {
        Browser browser = Browser.from(browserName != null ? browserName : ConfigReader.get("browser", "chrome"));
        boolean usePrivate = privateMode != null ? privateMode : isPrivateModeEnabled(browser);
        String execution = ConfigReader.get("execution", "local");

        LOG.info("Starting {} (private mode: {}, execution: {})", browser, usePrivate, execution);
        MutableCapabilities options = buildOptions(browser, usePrivate);

        WebDriver driver = "browserstack".equalsIgnoreCase(execution)
                ? BrowserStack.createDriver(options)
                : createLocalDriver(browser, options);

        configure(driver);
        DRIVER.set(driver);
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }

    /** A browser-specific key (e.g. "edge.private.mode") wins over the global "private.mode". */
    private static boolean isPrivateModeEnabled(Browser browser) {
        boolean global = ConfigReader.getBoolean("private.mode", false);
        return ConfigReader.getBoolean(browser.key() + ".private.mode", global);
    }

    private static MutableCapabilities buildOptions(Browser browser, boolean privateMode) {
        boolean headless = ConfigReader.getBoolean("headless", false);
        List<String> extraArgs = extraArguments(browser);

        switch (browser) {
            case CHROME: {
                ChromeOptions options = new ChromeOptions();
                options.setExperimentalOption("prefs", passwordManagerOffPrefs());
                options.addArguments("--disable-features=PasswordLeakDetection");
                if (privateMode) {
                    options.addArguments("--incognito");
                }
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                options.addArguments(extraArgs);
                return options;
            }
            case EDGE: {
                EdgeOptions options = new EdgeOptions();
                options.setExperimentalOption("prefs", passwordManagerOffPrefs());
                options.addArguments("--disable-features=PasswordLeakDetection");
                if (privateMode) {
                    options.addArguments("-inprivate");
                }
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                options.addArguments(extraArgs);
                return options;
            }
            case FIREFOX: {
                FirefoxOptions options = new FirefoxOptions();
                if (privateMode) {
                    options.addArguments("-private");
                }
                if (headless) {
                    options.addArguments("-headless");
                }
                options.addArguments(extraArgs);
                return options;
            }
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
    }

    /**
     * Stops the "save password" and "change your password" (breach warning) pop-ups.
     * They are browser UI, not part of the page, so tests cannot dismiss them.
     */
    private static Map<String, Object> passwordManagerOffPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        return prefs;
    }

    /** Comma-separated extra arguments from config, e.g. chrome.args=--disable-extensions,--lang=en-US */
    private static List<String> extraArguments(Browser browser) {
        String raw = ConfigReader.get(browser.key() + ".args", "");
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(arg -> !arg.isEmpty())
                .toList();
    }

    private static WebDriver createLocalDriver(Browser browser, MutableCapabilities options) {
        switch (browser) {
            case CHROME:
                return new ChromeDriver((ChromeOptions) options);
            case EDGE:
                return new EdgeDriver((EdgeOptions) options);
            case FIREFOX:
                return new FirefoxDriver((FirefoxOptions) options);
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
    }

    private static void configure(WebDriver driver) {
        int pageLoad = ConfigReader.getInt("page.load.timeout", FrameworkConstants.DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoad));
        // Implicit wait stays at 0 on purpose: mixing it with explicit waits causes unpredictable delays.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        if (!ConfigReader.getBoolean("headless", false)) {
            driver.manage().window().maximize();
        }
    }
}
