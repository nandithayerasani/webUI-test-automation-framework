package base;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import utils.ConfigReader;
import utils.LoggerUtil;

/**
 * Every test class extends this. It opens a fresh browser before each test method
 * and closes it afterwards.
 *
 * The browser and private mode can be set per &lt;test&gt; in testng.xml:
 *   &lt;parameter name="browser" value="edge"/&gt;
 *   &lt;parameter name="privateMode" value="true"/&gt;
 * When a parameter is missing, the value from config.properties is used.
 */
public abstract class BaseTest {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "privateMode"})
    public void setUp(@Optional String browser, @Optional String privateMode) {
        Boolean usePrivate = isUnset(privateMode) ? null : Boolean.valueOf(privateMode);
        DriverFactory.initDriver(isUnset(browser) ? null : browser, usePrivate);
        getDriver().get(ConfigReader.get("base.url"));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    private static boolean isUnset(String value) {
        return value == null || value.isBlank() || "null".equals(value);
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }
}
