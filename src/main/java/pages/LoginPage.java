package pages;

import base.BasePage;
import org.openqa.selenium.By;
import utils.ConfigReader;

public class LoginPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By USERNAME_INPUT = xpath("//input[@id='user-name']");
    private static final By PASSWORD_INPUT = xpath("//input[@id='password']");
    private static final By LOGIN_BUTTON   = xpath("//input[@id='login-button']");
    private static final By ERROR_MESSAGE  = xpath("//h3[@data-test='error']");
    private static final By LOGIN_LOGO     = xpath("//div[@class='login_logo']");
    // ==========================================================

    public LoginPage open() {
        driver.get(ConfigReader.get("base.url"));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(LOGIN_BUTTON);
    }

    public LoginPage enterUsername(String username) {
        type(USERNAME_INPUT, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(PASSWORD_INPUT, password);
        return this;
    }

    public void clickLogin() {
        click(LOGIN_BUTTON);
    }

    /** Logs in and returns the dashboard. Use attemptLogin when a failure is expected. */
    public DashboardPage loginAs(String username, String password) {
        attemptLogin(username, password);
        return new DashboardPage();
    }

    public LoginPage attemptLogin(String username, String password) {
        log.info("Logging in as {}", username);
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return this;
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }

    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }

    public boolean isLogoDisplayed() {
        return isDisplayed(LOGIN_LOGO);
    }
}
