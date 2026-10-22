# webUI-test-automation-framework

-  This is a pure TestNG and Page Object Model based framework
-  Preferable for automation-heavy and technical-teams

## Stack

Java 17, Maven, Selenium 4, TestNG, Log4j2, ExtentReports. Selenium Manager downloads browser drivers automatically, so no driver setup is needed.

## Project layout

```
src/main/java
  base/        DriverFactory (thread-safe driver, private mode), BrowserStack, BaseTest, BasePage
  constants/   FrameworkConstants (paths, defaults)
  pages/       Page objects: Login, Dashboard, Header, Footer, Settings (side menu)
  utils/       ConfigReader, WaitUtils, ScreenshotUtil, LoggerUtil
src/test/java
  listeners/   TestListener (logging, Extent report, screenshot on failure)
  tests/       Test classes
src/test/resources
  config.properties, testng.xml, browserstack.json, log4j2.xml
```

## Running

Only a JDK (17+) is needed. The Maven Wrapper downloads Maven on first use, so use `.\mvnw.cmd` on Windows or `./mvnw` on macOS/Linux in place of `mvn` if Maven is not installed.

```
mvn test                                        # uses config.properties
mvn test -Dbrowser=edge -Dprivate.mode=true     # Edge InPrivate
mvn test -Dbrowser=chrome -Dheadless=true
mvn test -Dexecution=browserstack               # needs BROWSERSTACK_USERNAME / BROWSERSTACK_ACCESS_KEY
```

Any key in `config.properties` can be overridden with `-Dkey=value`.

Reports: `reports/ExtentReport.html`. Failure screenshots: `reports/screenshots/`. Logs: `logs/automation.log`.

## CI pipelines

The pipeline files live in this repo so they are versioned together with the tests:

- `Jenkinsfile` for Jenkins. Point a Pipeline job at this repo with "Pipeline script from SCM". Set the JDK and Maven tool names to match your Jenkins (`JDK17`, `Maven3`). For BrowserStack, add a "Username with password" credential with id `browserstack-credentials`.
- `azure-pipelines.yml` for Azure DevOps. Create a pipeline from this file. For BrowserStack, add a variable group `browserstack` with secret variables `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY`.

Both let you pick browser, private mode, headless, execution (local or BrowserStack) and the suite file when you start a run. They default to headless, since build agents have no screen. Test results show up in the pipeline's test tab, and the Extent report and screenshots are saved as build artifacts.

## Private / incognito mode

| Browser | Argument used |
|---------|---------------|
| Chrome  | `--incognito` |
| Edge    | `-inprivate`  |
| Firefox | `-private`    |

Ways to turn it on, from strongest to weakest:

1. `privateMode` parameter on a `<test>` in testng.xml
2. A browser-specific key in config, e.g. `edge.private.mode=true`
3. The global key `private.mode=true` (or `-Dprivate.mode=true`)

Extra browser arguments can be added per browser with `chrome.args`, `edge.args` and `firefox.args` (comma-separated).

## Writing a page object

Keep every XPath together in the Locators section at the top of the class, then the actions below:

```java
public class LoginPage extends BasePage {

    // ==================== Locators (XPath) ====================
    private static final By USERNAME_INPUT = xpath("//input[@id='user-name']");
    private static final By LOGIN_BUTTON   = xpath("//input[@id='login-button']");
    // Dynamic: fill %s at run time with xpath(ROW_BY_NAME, name)
    private static final String ROW_BY_NAME = "//tr[td[text()='%s']]";
    // ==========================================================

    public void clickLogin() {
        click(LOGIN_BUTTON);
    }
}
```

`BasePage` provides `click`, `type`, `getText`, `isDisplayed`, `selectByVisibleText`, `jsClick`, `scrollIntoView` and more, all with explicit waits built in.

## Helpers

| Helper | What it does |
|--------|--------------|
| `WaitUtils` | Explicit waits, including `waitForStable` for elements that slide or animate |
| `WindowUtils` | Switch to a new pop-up window or tab, switch by title or URL, close and return, open tabs, frames |
| `AlertUtils` | JavaScript `alert`, `confirm` and `prompt`: accept, dismiss, read text, type, accept only if present |
| `FileUploadUtils` | Uploads through `<input type="file">`, including hidden inputs and multiple files. Relative names come from `src/test/resources/testdata`. Works on BrowserStack too. `BasePage.uploadFile(...)` wraps it |
| `MfaUtils` | Generates authenticator-app (TOTP) codes from the account's MFA secret, waiting for a fresh code if the current one is about to expire |
| `ScreenshotUtil` | Screenshots to file or Base64 |

Pop-up window example:

```java
String main = WindowUtils.getCurrentHandle();
click(OPEN_HELP_LINK);
WindowUtils.switchToNewWindow(main);
// ... work in the pop-up ...
WindowUtils.closeAndSwitchTo(main);
```

MFA example: when you enroll the test account, pick "can't scan the QR code" to see the secret key, and store it in the `MFA_SECRET` environment variable (or a pipeline secret). Then:

```java
type(MFA_CODE_INPUT, MfaUtils.getCode());
```

## Writing a test

Extend `BaseTest`. A fresh browser opens on `base.url` before each test method and closes afterwards. Add the class to `testng.xml`.
