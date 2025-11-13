package org.vaadin.tatu.vaadincreate;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deque.html.axecore.results.Results;
import com.vaadin.testbench.Parameters;
import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchTestCase;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.ComboBoxElement;
import com.vaadin.testbench.elements.FormLayoutElement;
import com.vaadin.testbench.elements.MenuBarElement;
import com.vaadin.testbench.elements.PasswordFieldElement;
import com.vaadin.testbench.elements.TextFieldElement;

/**
 * Base class for ITs
 * <p>
 * The tests use Chrome driver (see pom.xml for integration-tests profile) to
 * run integration tests on a headless Chrome. If a property {@code test.use
 * .hub} is set to true, {@code AbstractViewTest} will assume that the TestBench
 * test is running in a CI environment. In order to keep the this class light,
 * it makes certain assumptions about the CI environment (such as available
 * environment variables). It is not advisable to use this class as a base class
 * for you own TestBench tests.
 * <p>
 * To learn more about TestBench, visit <a href=
 * "https://vaadin.com/docs/v10/testbench/testbench-overview.html">Vaadin
 * TestBench</a>.
 */
public abstract class AbstractViewTest extends TestBenchTestCase {
    private static final int SERVER_PORT = 8080;

    private final String urlFragment;

    @Rule
    public ScreenshotOnFailureRule rule = new ScreenshotOnFailureRule(this,
            true);

    @BeforeClass
    public static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    public AbstractViewTest() {
        this("");
    }

    protected AbstractViewTest(String route) {
        this.urlFragment = route;
    }

    protected void open() {
        open((String[]) null);
    }

    protected void open(String... parameters) {
        String url = getTestURL(parameters);
        getDriver().get(url);
        waitForAppLoaded();
    }

    public void login(String user, String pass) {
        var loginForm = $(FormLayoutElement.class).first();
        loginForm.$(TextFieldElement.class).first().setValue(user);
        loginForm.$(PasswordFieldElement.class).first().setValue(pass);
        blur();
        loginForm.$(ComboBoxElement.class).first().getPopupSuggestionElements()
                .get(0).click();
        loginForm.$(ButtonElement.class).first().click();
        waitForElementPresent(By.className("applayout"));
    }

    public void logout() {
        $(MenuBarElement.class).first().findElement(By.id("logout-2")).click();
    }

    public void blur() {
        executeScript(
                "!!document.activeElement ? document.activeElement.blur() : 0");
    }

    /**
     * Returns the URL to the root of the server, e.g. "http://localhost:8888"
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return "http://" + getDeploymentHostname() + ":" + getDeploymentPort();
    }

    protected int getDeploymentPort() {
        return SERVER_PORT;
    }

    protected String getTestURL(String... parameters) {
        return getTestURL(getRootURL(), parameters);
    }

    public static String getTestURL(String rootUrl, String... parameters) {
        while (rootUrl.endsWith("/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
        }

        if (parameters != null && parameters.length != 0) {
            if (!rootUrl.contains("?")) {
                rootUrl += "?";
            } else {
                rootUrl += "&";
            }

            rootUrl += Arrays.stream(parameters)
                    .collect(Collectors.joining("&"));
        }

        return rootUrl;
    }

    @Before
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        // Dynamic sizing seems not work when @Viewport used in UI
        // Thus using ChromeOptions argument instead
        // of "testBench().resizeViewPortTo(1280, 900);" here.
        options.addArguments("--window-size=1280,900");
        if (Boolean.getBoolean("ghActions")) {
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
        }
        setDriver(TestBench.createDriver(new ChromeDriver(options)));
        getDriver().get(getURL(urlFragment));

        // We do screenshot testing, adjust settings to ensure less flakiness
        Parameters.setScreenshotComparisonTolerance(0.1);
        Parameters.setScreenshotComparisonCursorDetection(true);
        Parameters.setMaxScreenshotRetries(3);
        Parameters.setScreenshotRetryDelay(1000);

        // Wait for widgetset loaded before testing
        waitForAppLoaded();
    }

    // Return true to enable visual tests. They depend on used OS and Browser
    // version, hence disabled by default. Run tests once, screenshot tests
    // will fail, and new reference pictures will be generated in
    // error-screenshots folder. Check if they are ok and move to
    // reference-screenshots folder. Rerun the tests to verify,
    protected boolean visualTests() {
        if (Boolean.getBoolean("ghActions")) {
            return false;
        }
        return true;
    }

    public void waitForAppLoaded() {
        this.waitForElementPresent(By.id("AppWidgetset"));
    }

    public void logViolations(Results axeResults) {
        axeResults.getViolations().forEach(violation -> {
            logger.error("Accessibility violation: {} (ID: {})",
                    violation.getDescription(), violation.getId());
            violation.getNodes().forEach(node -> {
                logger.error("  Affected node: {}", node.getTarget());
            });
        });
    }

    /**
     * Returns deployment host name concatenated with route.
     *
     * @return URL to route
     */
    private static String getURL(String urlFragment) {
        return String.format("http://%s:%d/%s", getDeploymentHostname(),
                SERVER_PORT, urlFragment);
    }

    /**
     * Property set to true when running on a test hub.
     */
    private static final String USE_HUB_PROPERTY = "test.use.hub";

    /**
     * Returns whether we are using a test hub. This means that the starter is
     * running tests in Vaadin's CI environment, and uses TestBench to connect
     * to the testing hub.
     *
     * @return whether we are using a test hub
     */
    private static boolean isUsingHub() {
        return Boolean.TRUE.toString()
                .equals(System.getProperty(USE_HUB_PROPERTY));
    }

    /**
     * If running on CI, get the host name from environment variable HOSTNAME
     *
     * @return the host name
     */
    private static String getDeploymentHostname() {
        return isUsingHub() ? System.getenv("HOSTNAME") : "localhost";
    }

    protected void waitForElementPresent(final By by) {
        waitUntil(ExpectedConditions.presenceOfElementLocated(by));
    }

    protected void waitForElementNotPresent(final By by) {
        waitUntil(input -> input.findElements(by).isEmpty());
    }

    protected void waitForElementVisible(final By by) {
        waitUntil(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private static Logger logger = LoggerFactory
            .getLogger(AbstractViewTest.class);

}
