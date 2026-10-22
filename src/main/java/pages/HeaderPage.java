package pages;

import base.BasePage;
import org.openqa.selenium.By;

/** Header shared by every page after login. */
public class HeaderPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By MENU_BUTTON = xpath("//button[@id='react-burger-menu-btn']");
    private static final By APP_LOGO    = xpath("//div[@class='app_logo']");
    private static final By CART_LINK   = xpath("//a[@class='shopping_cart_link']");
    private static final By CART_BADGE  = xpath("//span[@class='shopping_cart_badge']");
    // ==========================================================

    public boolean isLogoDisplayed() {
        return isDisplayed(APP_LOGO);
    }

    public SettingsPage openMenu() {
        click(MENU_BUTTON);
        return new SettingsPage().waitUntilOpen();
    }

    public void openCart() {
        click(CART_LINK);
    }

    /** Number on the cart badge, 0 when the badge is not shown. */
    public int getCartCount() {
        return isPresentNow(CART_BADGE) ? Integer.parseInt(getText(CART_BADGE)) : 0;
    }
}
