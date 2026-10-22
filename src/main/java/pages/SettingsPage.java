package pages;

import base.BasePage;
import org.openqa.selenium.By;
import utils.WaitUtils;

/**
 * The side menu opened from the header (the demo site's settings and account actions).
 */
public class SettingsPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By MENU_PANEL      = xpath("//div[@class='bm-menu-wrap']");
    private static final By ALL_ITEMS_LINK  = xpath("//a[@id='inventory_sidebar_link']");
    private static final By ABOUT_LINK      = xpath("//a[@id='about_sidebar_link']");
    private static final By LOGOUT_LINK     = xpath("//a[@id='logout_sidebar_link']");
    private static final By RESET_LINK      = xpath("//a[@id='reset_sidebar_link']");
    private static final By CLOSE_BUTTON    = xpath("//button[@id='react-burger-cross-btn']");
    // ==========================================================

    /** The menu slides in, so wait for it to finish moving before clicking anything in it. */
    SettingsPage waitUntilOpen() {
        WaitUtils.waitForStable(LOGOUT_LINK);
        return this;
    }

    public boolean isOpen() {
        return isDisplayed(MENU_PANEL);
    }

    public DashboardPage goToAllItems() {
        click(ALL_ITEMS_LINK);
        return new DashboardPage();
    }

    public void goToAbout() {
        click(ABOUT_LINK);
    }

    public SettingsPage resetAppState() {
        click(RESET_LINK);
        return this;
    }

    public LoginPage logout() {
        click(LOGOUT_LINK);
        return new LoginPage();
    }

    public void close() {
        click(CLOSE_BUTTON);
        WaitUtils.waitForInvisible(LOGOUT_LINK);
    }
}
