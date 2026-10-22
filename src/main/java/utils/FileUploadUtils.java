package utils;

import base.DriverFactory;
import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Uploads files through an &lt;input type="file"&gt; without opening the OS file dialog
 * (Selenium cannot control that dialog).
 *
 * Relative paths are resolved against src/test/resources/testdata, so tests can just say
 *   FileUploadUtils.upload(FILE_INPUT, "sample-upload.txt");
 * On BrowserStack the file is sent from this machine to the remote browser automatically.
 */
public final class FileUploadUtils {

    private static final Logger LOG = LoggerUtil.getLogger(FileUploadUtils.class);

    private FileUploadUtils() {
    }

    /** Uploads one or more files. Multiple files need an input with the "multiple" attribute. */
    public static void upload(By fileInput, String... files) {
        if (files.length == 0) {
            throw new IllegalArgumentException("No files given to upload");
        }
        WebDriver driver = DriverFactory.getDriver();
        if (driver instanceof RemoteWebDriver remote && !isLocalDriver(remote)) {
            remote.setFileDetector(new LocalFileDetector());
        }

        String paths = Arrays.stream(files)
                .map(FileUploadUtils::resolve)
                .map(Path::toString)
                .collect(Collectors.joining("\n"));

        WebElement input = WaitUtils.waitForPresent(fileInput);
        makeVisibleIfHidden(driver, input);
        input.sendKeys(paths);
        LOG.info("Uploaded {}", String.join(", ", files));
    }

    /** Absolute path for a file, checking it exists. Relative names are looked up in testdata. */
    public static Path resolve(String file) {
        Path path = Path.of(file);
        if (!path.isAbsolute()) {
            path = FrameworkConstants.TEST_DATA_DIR.resolve(file);
        }
        path = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Upload file not found: " + path);
        }
        return path;
    }

    /**
     * Many sites hide the real file input behind a styled button. sendKeys needs it
     * to be interactable, so un-hide it.
     */
    private static void makeVisibleIfHidden(WebDriver driver, WebElement input) {
        if (!input.isDisplayed()) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.display='block';"
                            + "arguments[0].style.visibility='visible';"
                            + "arguments[0].style.opacity=1;"
                            + "arguments[0].style.height='1px';"
                            + "arguments[0].style.width='1px';", input);
        }
    }

    /** ChromeDriver, EdgeDriver and FirefoxDriver extend RemoteWebDriver too; only true remote sessions need the detector. */
    private static boolean isLocalDriver(RemoteWebDriver driver) {
        return driver.getClass() != RemoteWebDriver.class;
    }
}
