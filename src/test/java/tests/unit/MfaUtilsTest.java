package tests.unit;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.MfaUtils;

/** Checks the MFA code generator against the official RFC 6238 test values. No browser needed. */
public class MfaUtilsTest {

    // Base32 of the RFC test key "12345678901234567890"
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @DataProvider(name = "rfcVectors")
    public Object[][] rfcVectors() {
        return new Object[][]{
                {59L, "94287082"},
                {1111111109L, "07081804"},
                {1111111111L, "14050471"},
                {1234567890L, "89005924"},
                {2000000000L, "69279037"},
        };
    }

    @Test(dataProvider = "rfcVectors", groups = {"unit"})
    public void generatesRfcCodes(long time, String expected) {
        Assert.assertEquals(MfaUtils.generate(RFC_SECRET, time, 8), expected);
    }

    @Test(groups = {"unit"})
    public void sixDigitCodeIsLastSixDigits() {
        Assert.assertEquals(MfaUtils.generate(RFC_SECRET, 59L, 6), "287082");
    }

    @Test(groups = {"unit"})
    public void ignoresSpacesAndLowerCaseInSecret() {
        Assert.assertEquals(MfaUtils.generate("gezd gnbv gy3t qojq gezd gnbv gy3t qojq", 59L, 6), "287082");
    }
}
