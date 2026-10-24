package tests;

import base.BaseTest;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.ITestNGMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.LoginPage;
import utils.TestDataReader;

import java.util.Map;

/**
 * Test data comes from the "Login" sheet of the current environment's TestData.xlsx
 * (src/test/resources/testdata/<env>/TestData.xlsx).
 * Each test's first group is its Jira ID, so it can be run alone: mvn test -Dtags=nanditha-0001
 */
public class LoginTest extends BaseTest {

    private static final String SHEET = "Login";

    @Test(groups = {"nanditha-0001", "smoke", "login"})
    public void validLoginShowsDashboard() {
        Map<String, String> data = TestDataReader.getRow(SHEET, "nanditha-0001");

        DashboardPage dashboard = new LoginPage().loginAs(data.get("Username"), data.get("Password"));

        Assert.assertTrue(dashboard.isLoaded(), "Dashboard did not load");
        Assert.assertEquals(dashboard.getTitle(), "Products");
        Assert.assertTrue(dashboard.header().isLogoDisplayed(), "Header logo missing");
    }

    @DataProvider(name = "invalidLogins", parallel = true)
    public Object[][] invalidLogins(ITestNGMethod method) {
        return TestDataReader.asDataProvider(method, SHEET, row -> "invalid".equalsIgnoreCase(row.get("Scenario")));
    }

    /** One Jira ID per Excel row (nanditha-0002 to nanditha-0005); -Dtags can run a single row. */
    @Test(dataProvider = "invalidLogins", groups = {"regression", "login"})
    public void invalidLoginShowsError(Map<String, String> data) {
        TestListener.step(data.get("TestCaseId") + ": " + data.get("Description"));
        LoginPage loginPage = new LoginPage().attemptLogin(data.get("Username"), data.get("Password"));

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message not shown");
        Assert.assertTrue(loginPage.getErrorMessage().contains(data.get("ExpectedError")),
                "Unexpected error: " + loginPage.getErrorMessage());
    }

    @Test(groups = {"nanditha-0006", "smoke", "login"})
    public void logoutReturnsToLoginPage() {
        Map<String, String> data = TestDataReader.getRow(SHEET, "nanditha-0001");
        DashboardPage dashboard = new LoginPage().loginAs(data.get("Username"), data.get("Password"));
        TestListener.step("Logged in");

        LoginPage loginPage = dashboard.header().openMenu().logout();

        Assert.assertTrue(loginPage.isLoaded(), "Login page not shown after logout");
    }
}
