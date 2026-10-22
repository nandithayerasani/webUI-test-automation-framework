package base;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import utils.FileUploadUtils;
import utils.LoggerUtil;
import utils.WaitUtils;

import java.util.List;

/**
 * Parent of every page object. Holds the shared actions so pages only describe
 * their own locators and behaviour.
 *
 * Convention for page classes: keep all XPaths together in a "Locators" section
 * at the top of the class, then the actions below it.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Logger log = LoggerUtil.getLogger(getClass());

    protected BasePage() {
        this.driver = DriverFactory.getDriver();
    }

    /** Builds a By from an XPath, filling %s placeholders for dynamic locators. */
    protected static By xpath(String xpath, Object... args) {
        return By.xpath(args.length == 0 ? xpath : String.format(xpath, args));
    }

    protected void click(By locator) {
        log.debug("Click {}", locator);
        WaitUtils.waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        log.debug("Type into {}", locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitUtils.waitForVisible(locator).getText().trim();
    }

    protected String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForPresent(locator).getAttribute(attribute);
    }

    /** True if the element becomes visible within the default wait; never throws for a missing element. */
    protected boolean isDisplayed(By locator) {
        try {
            return WaitUtils.waitForVisible(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Immediate check with no waiting, for asserting something is absent. */
    protected boolean isPresentNow(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected List<WebElement> findAll(By locator) {
        return WaitUtils.waitForAllVisible(locator);
    }

    protected void selectByVisibleText(By locator, String text) {
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(text);
    }

    protected void selectByValue(By locator, String value) {
        new Select(WaitUtils.waitForVisible(locator)).selectByValue(value);
    }

    protected void jsClick(By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", WaitUtils.waitForPresent(locator));
    }

    protected void scrollIntoView(By locator) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});",
                WaitUtils.waitForPresent(locator));
    }

    /** Uploads files through a file input. Relative names come from src/test/resources/testdata. */
    protected void uploadFile(By fileInput, String... files) {
        FileUploadUtils.upload(fileInput, files);
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
