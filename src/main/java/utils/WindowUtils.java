package utils;

import base.DriverFactory;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Handles browser windows, tabs and frames for the current thread's driver.
 *
 * Typical pop-up window flow:
 *   String main = WindowUtils.getCurrentHandle();
 *   click(OPEN_POPUP_BUTTON);
 *   WindowUtils.switchToNewWindow(main);
 *   ... work in the pop-up ...
 *   WindowUtils.closeAndSwitchTo(main);
 */
public final class WindowUtils {

    private static final Logger LOG = LoggerUtil.getLogger(WindowUtils.class);

    private WindowUtils() {
    }

    private static WebDriver driver() {
        return DriverFactory.getDriver();
    }

    public static String getCurrentHandle() {
        return driver().getWindowHandle();
    }

    public static Set<String> getAllHandles() {
        return driver().getWindowHandles();
    }

    public static void waitForWindowCount(int count) {
        WaitUtils.getWait().until(ExpectedConditions.numberOfWindowsToBe(count));
    }

    /**
     * Waits for a window other than the given one to open and switches to it.
     * Call getCurrentHandle() before the action that opens the pop-up.
     */
    public static String switchToNewWindow(String originalHandle) {
        String handle = WaitUtils.getWait().until(d -> d.getWindowHandles().stream()
                .filter(h -> !h.equals(originalHandle))
                .findFirst()
                .orElse(null));
        driver().switchTo().window(handle);
        LOG.info("Switched to new window: {}", driver().getTitle());
        return handle;
    }

    public static void switchToWindow(String handle) {
        driver().switchTo().window(handle);
    }

    public static void switchToWindowByTitle(String titlePart) {
        switchToWindowMatching(d -> d.getTitle() != null && d.getTitle().contains(titlePart), "title containing " + titlePart);
    }

    public static void switchToWindowByUrl(String urlPart) {
        switchToWindowMatching(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(urlPart), "URL containing " + urlPart);
    }

    private static void switchToWindowMatching(Predicate<WebDriver> matcher, String description) {
        String original = getCurrentHandle();
        try {
            WaitUtils.getWait().until(d -> {
                for (String handle : d.getWindowHandles()) {
                    d.switchTo().window(handle);
                    if (matcher.test(d)) {
                        return true;
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            driver().switchTo().window(original);
            throw new NoSuchWindowException("No window with " + description);
        }
        LOG.info("Switched to window with {}", description);
    }

    /** Closes the current window or tab and switches back to the given one. */
    public static void closeAndSwitchTo(String handle) {
        driver().close();
        driver().switchTo().window(handle);
    }

    /** Closes every window except the given one and switches to it. */
    public static void closeAllExcept(String handle) {
        for (String h : getAllHandles()) {
            if (!h.equals(handle)) {
                driver().switchTo().window(h).close();
            }
        }
        driver().switchTo().window(handle);
    }

    /** Opens a blank tab, switches to it and loads the URL. */
    public static String openNewTab(String url) {
        driver().switchTo().newWindow(WindowType.TAB);
        driver().get(url);
        return getCurrentHandle();
    }

    public static String openNewWindow(String url) {
        driver().switchTo().newWindow(WindowType.WINDOW);
        driver().get(url);
        return getCurrentHandle();
    }

    // ==================== Frames ====================

    public static void switchToFrame(By frameLocator) {
        WaitUtils.getWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
    }

    public static void switchToFrame(String nameOrId) {
        WaitUtils.getWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    public static void switchToParentFrame() {
        driver().switchTo().parentFrame();
    }

    public static void switchToDefaultContent() {
        driver().switchTo().defaultContent();
    }
}
