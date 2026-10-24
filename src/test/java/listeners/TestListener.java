package listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ConfigReader;
import utils.LoggerUtil;
import utils.ScreenshotUtil;
import utils.TagFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Logs each test, builds the Extent HTML report (reports/ExtentReport.html) and
 * attaches a screenshot when a test fails. Registered in testng.xml.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LoggerUtil.getLogger(TestListener.class);
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static ExtentReports extent;

    @Override
    public synchronized void onStart(ISuite suite) {
        if (extent != null) {
            return;
        }
        ExtentSparkReporter spark = new ExtentSparkReporter(
                FrameworkConstants.REPORTS_DIR.resolve(FrameworkConstants.EXTENT_REPORT_NAME).toString());
        spark.config().setDocumentTitle("Web UI Automation Report");
        spark.config().setReportName(suite.getName());

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", ConfigReader.getEnv().toUpperCase());
        extent.setSystemInfo("Base URL", ConfigReader.get("base.url", ""));
        extent.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
        extent.setSystemInfo("Private mode", ConfigReader.get("private.mode", "false"));
        extent.setSystemInfo("Execution", ConfigReader.get("execution", "local"));
        if (TagFilter.isFiltering()) {
            extent.setSystemInfo("Tags", String.join(", ", TagFilter.activeTags()));
        }
    }

    @Override
    public synchronized void onFinish(ISuite suite) {
        if (extent != null) {
            extent.flush();
            LOG.info("Report written to {}", FrameworkConstants.REPORTS_DIR.resolve(FrameworkConstants.EXTENT_REPORT_NAME));
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = testName(result);
        LOG.info("START  {}", name);
        ExtentTest test = extent.createTest(name, result.getMethod().getDescription());
        test.assignCategory(result.getMethod().getGroups());
        CURRENT_TEST.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS   {}", testName(result));
        CURRENT_TEST.get().log(Status.PASS, "Passed");
        CURRENT_TEST.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String name = testName(result);
        LOG.error("FAIL   {}", name, result.getThrowable());
        ExtentTest test = CURRENT_TEST.get();
        test.fail(result.getThrowable());

        if (ConfigReader.getBoolean("screenshot.on.failure", true)) {
            byte[] png = ScreenshotUtil.captureBytes();
            if (png != null) {
                ScreenshotUtil.save(name, png);
                test.fail("Screenshot at failure", MediaEntityBuilder
                        .createScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(png)).build());
            }
        }
        CURRENT_TEST.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIP   {}", testName(result));
        ExtentTest test = CURRENT_TEST.get();
        if (test == null) {
            test = extent.createTest(testName(result));
        }
        test.skip(result.getThrowable() != null ? result.getThrowable().getMessage() : "Skipped");
        CURRENT_TEST.remove();
    }

    /** Lets tests add their own steps to the report: TestListener.step("Opened cart"). */
    public static void step(String message) {
        ExtentTest test = CURRENT_TEST.get();
        if (test != null) {
            test.info(message);
        }
    }

    /** "nanditha-0345 | LoginTest.validLogin": Jira IDs from groups or the Excel row come first. */
    private static String testName(ITestResult result) {
        String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        String ids = jiraIds(result);
        return ids.isEmpty() ? name : ids + " | " + name;
    }

    private static String jiraIds(ITestResult result) {
        List<String> ids = new ArrayList<>();
        Arrays.stream(result.getMethod().getGroups()).filter(TagFilter::isJiraKey).forEach(ids::add);
        for (Object parameter : result.getParameters()) {
            if (parameter instanceof Map<?, ?> row && row.get("TestCaseId") instanceof String id && TagFilter.isJiraKey(id)) {
                ids.add(id);
            }
        }
        return String.join(", ", ids);
    }
}
