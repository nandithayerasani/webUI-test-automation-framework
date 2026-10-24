package tests.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import utils.TestDataReader;

import java.util.List;
import java.util.Map;

/** Checks the Excel reader against the sample QA TestData.xlsx. No browser needed. */
public class TestDataReaderTest {

    @Test(groups = {"unit"})
    public void readsRowById() {
        Map<String, String> row = TestDataReader.getRow("Login", "nanditha-0001");
        Assert.assertEquals(row.get("Username"), "standard_user");
    }

    @Test(groups = {"unit"})
    public void skipsRowsMarkedN() {
        List<Map<String, String>> rows = TestDataReader.getRows("Login");
        Assert.assertTrue(rows.stream().noneMatch(r -> "nanditha-0005".equals(r.get("TestCaseId"))));
    }

    @Test(groups = {"unit"})
    public void numbersComeBackAsShownInExcel() {
        Assert.assertEquals(TestDataReader.getRow("Products", "nanditha-0102").get("Price"), "9.99");
    }

    @Test(groups = {"unit"})
    public void dataProviderHasOneMapPerRow() {
        Object[][] data = TestDataReader.asDataProvider(null, "Login", r -> "invalid".equals(r.get("Scenario")));
        Assert.assertEquals(data.length, 3);
    }
}
