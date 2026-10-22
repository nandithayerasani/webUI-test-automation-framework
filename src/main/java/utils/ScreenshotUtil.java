package utils;

import base.DriverFactory;
import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots from the current thread's driver.
 */
public final class ScreenshotUtil {

    private static final Logger LOG = LoggerUtil.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {
    }

    /** Saves a PNG under reports/screenshots and returns its path, or null if it failed. */
    public static Path capture(String name) {
        try {
            byte[] png = ((TakesScreenshot) DriverFactory.getDriver()).getScreenshotAs(OutputType.BYTES);
            Files.createDirectories(FrameworkConstants.SCREENSHOTS_DIR);
            String fileName = name.replaceAll("[^a-zA-Z0-9._-]", "_") + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
            Path target = FrameworkConstants.SCREENSHOTS_DIR.resolve(fileName);
            Files.write(target, png);
            LOG.info("Screenshot saved: {}", target);
            return target;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not capture screenshot '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /** Base64 PNG, handy for embedding straight into the HTML report. */
    public static String captureBase64() {
        try {
            return ((TakesScreenshot) DriverFactory.getDriver()).getScreenshotAs(OutputType.BASE64);
        } catch (RuntimeException e) {
            LOG.warn("Could not capture base64 screenshot: {}", e.getMessage());
            return null;
        }
    }
}
