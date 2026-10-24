package base;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import utils.ConfigReader;
import utils.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs tests on BrowserStack instead of a local browser (set execution=browserstack).
 * Platform settings come from browserstack.json. Credentials come from the
 * BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY environment variables, or
 * browserstack.username / browserstack.accesskey system properties. Never commit them.
 */
public final class BrowserStack {

    private static final Logger LOG = LoggerUtil.getLogger(BrowserStack.class);
    private static final String DEFAULT_HUB = "https://hub.browserstack.com/wd/hub";

    private BrowserStack() {
    }

    @SuppressWarnings("unchecked")
    static WebDriver createDriver(MutableCapabilities browserOptions) {
        Map<String, Object> config = readConfig();

        Map<String, Object> capabilities = (Map<String, Object>) config.getOrDefault("capabilities", new HashMap<>());
        Map<String, Object> bstackOptions = new HashMap<>(
                (Map<String, Object>) capabilities.getOrDefault("bstack:options", new HashMap<>()));
        bstackOptions.put("userName", ConfigReader.getSecret("BROWSERSTACK_USERNAME", "browserstack.username"));
        bstackOptions.put("accessKey", ConfigReader.getSecret("BROWSERSTACK_ACCESS_KEY", "browserstack.accesskey"));

        capabilities.forEach((key, value) -> {
            if (!"bstack:options".equals(key)) {
                browserOptions.setCapability(key, value);
            }
        });
        browserOptions.setCapability("bstack:options", bstackOptions);

        String hub = String.valueOf(config.getOrDefault("hubUrl", DEFAULT_HUB));
        LOG.info("Starting BrowserStack session on {}", hub);
        try {
            return new RemoteWebDriver(new URL(hub), browserOptions);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid BrowserStack hub URL: " + hub, e);
        }
    }

    private static Map<String, Object> readConfig() {
        try (InputStream in = BrowserStack.class.getClassLoader()
                .getResourceAsStream(FrameworkConstants.BROWSERSTACK_CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(FrameworkConstants.BROWSERSTACK_CONFIG_FILE + " not found on the classpath");
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FrameworkConstants.BROWSERSTACK_CONFIG_FILE, e);
        }
    }
}
