package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Products page shown after a successful login. */
public class DashboardPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By PAGE_TITLE      = xpath("//span[@class='title']");
    private static final By INVENTORY_LIST  = xpath("//div[@class='inventory_list']");
    private static final By INVENTORY_ITEMS = xpath("//div[@class='inventory_item']");
    private static final By ITEM_NAMES      = xpath("//div[@data-test='inventory-item-name']");
    private static final By SORT_DROPDOWN   = xpath("//select[@class='product_sort_container']");

    // Dynamic: fill %s with the product name
    private static final String ADD_TO_CART_BUTTON =
            "//div[text()='%s']/ancestor::div[@class='inventory_item']//button[starts-with(@id,'add-to-cart')]";
    private static final String REMOVE_BUTTON =
            "//div[text()='%s']/ancestor::div[@class='inventory_item']//button[starts-with(@id,'remove')]";
    // ==========================================================

    private final HeaderPage header = new HeaderPage();
    private final FooterPage footer = new FooterPage();

    public boolean isLoaded() {
        return isDisplayed(INVENTORY_LIST);
    }

    public String getTitle() {
        return getText(PAGE_TITLE);
    }

    public int getProductCount() {
        return findAll(INVENTORY_ITEMS).size();
    }

    public List<String> getProductNames() {
        return findAll(ITEM_NAMES).stream().map(WebElement::getText).toList();
    }

    public DashboardPage sortBy(String visibleText) {
        selectByVisibleText(SORT_DROPDOWN, visibleText);
        return this;
    }

    public DashboardPage addToCart(String productName) {
        click(xpath(ADD_TO_CART_BUTTON, productName));
        return this;
    }

    public DashboardPage removeFromCart(String productName) {
        click(xpath(REMOVE_BUTTON, productName));
        return this;
    }

    public HeaderPage header() {
        return header;
    }

    public FooterPage footer() {
        return footer;
    }
}
