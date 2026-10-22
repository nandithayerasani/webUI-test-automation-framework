package tests;

import base.BaseTest;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.LoginPage;
import utils.ConfigReader;

public class LoginTest extends BaseTest {

    @Test(description = "Valid user lands on the products page", groups = {"smoke", "login"})
    public void validLoginShowsDashboard() {
        DashboardPage dashboard = new LoginPage()
                .loginAs(ConfigReader.get("username"), ConfigReader.get("password"));

        Assert.assertTrue(dashboard.isLoaded(), "Dashboard did not load");
        Assert.assertEquals(dashboard.getTitle(), "Products");
        Assert.assertTrue(dashboard.header().isLogoDisplayed(), "Header logo missing");
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][]{
                {"locked_out_user", "secret_sauce", "Sorry, this user has been locked out."},
                {"standard_user", "wrong_password", "Username and password do not match"},
                {"", "secret_sauce", "Username is required"},
        };
    }

    @Test(description = "Invalid logins show the right error", dataProvider = "invalidCredentials",
            groups = {"regression", "login"})
    public void invalidLoginShowsError(String username, String password, String expectedError) {
        LoginPage loginPage = new LoginPage().attemptLogin(username, password);

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message not shown");
        Assert.assertTrue(loginPage.getErrorMessage().contains(expectedError),
                "Unexpected error: " + loginPage.getErrorMessage());
    }

    @Test(description = "User can log out from the side menu", groups = {"smoke", "login"})
    public void logoutReturnsToLoginPage() {
        DashboardPage dashboard = new LoginPage()
                .loginAs(ConfigReader.get("username"), ConfigReader.get("password"));
        TestListener.step("Logged in");

        LoginPage loginPage = dashboard.header().openMenu().logout();

        Assert.assertTrue(loginPage.isLoaded(), "Login page not shown after logout");
    }
}
