package pages;

import base.BasePage;
import org.openqa.selenium.By;

/** Footer shared by every page after login. */
public class FooterPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By FOOTER        = xpath("//footer[@class='footer']");
    private static final By TWITTER_LINK  = xpath("//li[@class='social_twitter']/a");
    private static final By FACEBOOK_LINK = xpath("//li[@class='social_facebook']/a");
    private static final By LINKEDIN_LINK = xpath("//li[@class='social_linkedin']/a");
    private static final By COPYRIGHT     = xpath("//div[@class='footer_copy']");
    // ==========================================================

    public boolean isDisplayed() {
        scrollIntoView(FOOTER);
        return isDisplayed(FOOTER);
    }

    public String getCopyrightText() {
        scrollIntoView(COPYRIGHT);
        return getText(COPYRIGHT);
    }

    public String getTwitterUrl() {
        return getAttribute(TWITTER_LINK, "href");
    }

    public String getFacebookUrl() {
        return getAttribute(FACEBOOK_LINK, "href");
    }

    public String getLinkedInUrl() {
        return getAttribute(LINKEDIN_LINK, "href");
    }
}
