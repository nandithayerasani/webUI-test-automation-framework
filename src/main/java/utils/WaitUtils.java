package utils;

import base.DriverFactory;
import constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Explicit waits for the current thread's driver. Timeout comes from "explicit.wait" in config.
 */
public final class WaitUtils {

    private WaitUtils() {
    }

    private static WebDriverWait getWait() {
        return getWait(ConfigReader.getInt("explicit.wait", FrameworkConstants.DEFAULT_EXPLICIT_WAIT_SECONDS));
    }

    private static WebDriverWait getWait(int seconds) {
        return new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(seconds));
    }

    public static WebElement waitForVisible(By locator) {
        return getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible(By locator, int seconds) {
        return getWait(seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return getWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForPresent(By locator) {
        return getWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        return getWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return getWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForUrlContains(String fragment) {
        return getWait().until(ExpectedConditions.urlContains(fragment));
    }

    public static boolean waitForTitleContains(String title) {
        return getWait().until(ExpectedConditions.titleContains(title));
    }

    /**
     * Waits until the element stops moving, e.g. a menu that slides in.
     * Clicking during a CSS animation can land on the wrong spot and be silently ignored.
     */
    public static WebElement waitForStable(By locator) {
        WebElement element = waitForVisible(locator);
        Rectangle[] last = {null};
        getWait().pollingEvery(Duration.ofMillis(150)).until(driver -> {
            Rectangle current = element.getRect();
            boolean stable = current.equals(last[0]);
            last[0] = current;
            return stable;
        });
        return element;
    }

    /** Waits until document.readyState is "complete". */
    public static void waitForPageLoad() {
        getWait().until(driver -> "complete".equals(
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return document.readyState")));
    }
}
