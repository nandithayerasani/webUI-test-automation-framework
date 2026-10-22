package utils;

import base.DriverFactory;
import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Handles JavaScript pop-ups: alert(), confirm() and prompt().
 * For browser login pop-ups (basic auth) put the credentials in the URL instead:
 * https://user:password@host/path
 */
public final class AlertUtils {

    private static final Logger LOG = LoggerUtil.getLogger(AlertUtils.class);

    private AlertUtils() {
    }

    private static Alert waitForAlert(int seconds) {
        return new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(seconds))
                .until(ExpectedConditions.alertIsPresent());
    }

    public static Alert waitForAlert() {
        return waitForAlert(ConfigReader.getInt("explicit.wait", FrameworkConstants.DEFAULT_EXPLICIT_WAIT_SECONDS));
    }

    /** True if an alert shows up within the given seconds. Never throws. */
    public static boolean isAlertPresent(int seconds) {
        try {
            waitForAlert(seconds);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Clicks OK and returns the alert text. */
    public static String accept() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        LOG.info("Accepted alert: {}", text);
        return text;
    }

    /** Clicks Cancel and returns the alert text. */
    public static String dismiss() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.dismiss();
        LOG.info("Dismissed alert: {}", text);
        return text;
    }

    public static String getText() {
        return waitForAlert().getText();
    }

    /** Types into a prompt() and clicks OK. */
    public static void sendTextAndAccept(String text) {
        Alert alert = waitForAlert();
        alert.sendKeys(text);
        alert.accept();
    }

    /** Accepts an alert only if one appears within the given seconds, e.g. an occasional "Are you sure?". */
    public static void acceptIfPresent(int seconds) {
        if (isAlertPresent(seconds)) {
            accept();
        }
    }
}
