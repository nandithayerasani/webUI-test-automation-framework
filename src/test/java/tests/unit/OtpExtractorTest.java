package tests.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import utils.email.OtpExtractor;
import utils.email.OtpQuery;

import java.util.Optional;
import java.util.regex.Pattern;

/** Checks that codes are pulled out of typical OTP emails. No mailbox needed. */
public class OtpExtractorTest {

    @Test(groups = {"unit"})
    public void findsCodeInPlainText() {
        Assert.assertEquals(OtpExtractor.extract("Your verification code is 482913. It expires in 10 minutes.",
                OtpQuery.DEFAULT_PATTERN), Optional.of("482913"));
    }

    @Test(groups = {"unit"})
    public void ignoresCssColoursInHtml() {
        String html = "<html><head><style>p { color: #123456; }</style></head>"
                + "<body><p>Your code:</p><p><b>739201</b></p></body></html>";
        Assert.assertEquals(OtpExtractor.extract(html, OtpQuery.DEFAULT_PATTERN), Optional.of("739201"));
    }

    @Test(groups = {"unit"})
    public void customPatternPicksTheRightNumber() {
        String body = "Order 2024 confirmed. Use code 55123 to sign in.";
        Assert.assertEquals(OtpExtractor.extract(body, Pattern.compile("code (\\d{5})")), Optional.of("55123"));
    }

    @Test(groups = {"unit"})
    public void returnsEmptyWhenNoCode() {
        Assert.assertTrue(OtpExtractor.extract("Welcome aboard!", OtpQuery.DEFAULT_PATTERN).isEmpty());
    }
}
