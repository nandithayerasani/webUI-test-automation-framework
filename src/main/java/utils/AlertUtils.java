package utils;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Handles JavaScript pop-ups: alert(), confirm() and prompt().
 * For browser login pop-ups (basic auth) put the credentials in the URL instead:
 * https://user:password@host/path
 */
public final class AlertUtils {

    private static final Logger LOG = LoggerUtil.getLogger(AlertUtils.class);

    private AlertUtils() {
    }

    public static Alert waitForAlert() {
        return WaitUtils.getWait().until(ExpectedConditions.alertIsPresent());
    }

    /** True if an alert shows up within the given seconds. Never throws. */
    public static boolean isAlertPresent(int seconds) {
        try {
            WaitUtils.getWait(seconds).until(ExpectedConditions.alertIsPresent());
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
