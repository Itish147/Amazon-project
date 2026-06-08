package com.amazon.tests;

import com.amazon.config.DriverConfig;
import com.amazon.config.LambdaTestConfig;
import com.amazon.pages.CartPage;
import com.amazon.pages.HomePage;
import com.amazon.pages.ProductPage;
import com.amazon.pages.SearchResultsPage;
import com.amazon.utils.ExtentReportManager;
import com.amazon.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProductSearchCartTest
 * ════════════════════════════════════════════════════════════════════════════
 * Parameterised TestNG test class that covers:
 *
 * <ul>
 *   <li><b>TC-01</b> – Search for an iPhone, add to cart, print price.</li>
 *   <li><b>TC-02</b> – Search for a Galaxy device, add to cart, print price.</li>
 * </ul>
 *
 * <p>Both test cases are executed <em>in parallel</em> by TestNG, driven by
 * {@code src/test/resources/testng-parallel.xml}:</p>
 * <pre>
 *   parallel="tests"   – each {@code <test>} block runs in its own thread
 *   thread-count="2"   – two threads run concurrently
 * </pre>
 *
 * <p>Thread isolation is guaranteed by {@link DriverConfig}, which stores the
 * {@link WebDriver} in a {@link ThreadLocal} so TC-01 and TC-02 never share
 * a browser session.</p>
 *
 * <h3>Run locally</h3>
 * <pre>
 *   mvn test
 * </pre>
 *
 * <h3>Run on LambdaTest</h3>
 * <pre>
 *   LT_USERNAME=&lt;u&gt; LT_ACCESS_KEY=&lt;k&gt; mvn test -DuseLambdaTest=true
 * </pre>
 */
public class ProductSearchCartTest {

    private static final Logger log =
            LoggerFactory.getLogger(ProductSearchCartTest.class);

    // ── Suite-level hooks (run once across all threads) ───────────────────

    @BeforeSuite(alwaysRun = true)
    public void initReport() {
        ExtentReportManager.init();
        log.info("══════════════════════════════════════════════");
        log.info("  Amazon Automation Suite  –  STARTING");
        log.info("══════════════════════════════════════════════");
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownReport() {
        ExtentReportManager.flush();
        log.info("══════════════════════════════════════════════");
        log.info("  Amazon Automation Suite  –  COMPLETE");
        log.info("══════════════════════════════════════════════");
    }

    // ── Test-level hooks (run per thread) ─────────────────────────────────

    /**
     * Initialise a WebDriver for this thread.
     *
     * @param searchQuery  injected from testng-parallel.xml {@code <parameter>}
     * @param productLabel human-readable label for reporting (e.g. "iPhone")
     */
    @BeforeMethod(alwaysRun = true)
    @Parameters({"searchQuery", "productLabel"})
    public void setUp(String searchQuery, String productLabel) {
        log.info("[Thread-{}] ──── Setting up for: '{}' ────",
                Thread.currentThread().getId(), productLabel);

        boolean useLambdaTest =
                Boolean.parseBoolean(System.getProperty("useLambdaTest", "false"));

        if (useLambdaTest) {
            // ── Cloud execution (LambdaTest) ───────────────────────────
            WebDriver cloudDriver = LambdaTestConfig.createDriver(
                    "Amazon Cart – " + productLabel);
            // Manually inject into DriverConfig's ThreadLocal via initDriver
            // We expose a package-visible setter for this purpose.
            DriverConfig.setDriver(cloudDriver);
            log.info("[Thread-{}] LambdaTest RemoteWebDriver ready.",
                    Thread.currentThread().getId());
        } else {
            // ── Local execution ────────────────────────────────────────
            DriverConfig.initDriver();
        }

        // Create an Extent report node for this test thread
        ExtentReportManager.createTest("TC | " + productLabel + " Search & Cart");
        ExtentReportManager.getTest()
                .info("Search query: <b>" + searchQuery + "</b>");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Capture screenshot on failure
        if (result.getStatus() == ITestResult.FAILURE) {
            try {
                String path = ScreenshotUtil.capture(
                        DriverConfig.getDriver(), result.getName());
                ExtentReportManager.getTest()
                        .fail("Test FAILED. Screenshot → " + path);
                ExtentReportManager.getTest()
                        .fail(result.getThrowable());
            } catch (Exception e) {
                log.warn("Could not capture failure screenshot: {}", e.getMessage());
            }
        }

        DriverConfig.quitDriver();
        ExtentReportManager.removeTest();
        log.info("[Thread-{}] Driver quit. Status: {}",
                Thread.currentThread().getId(),
                result.getStatus() == ITestResult.SUCCESS ? "PASS ✓" : "FAIL ✗");
    }

    // ── Test ──────────────────────────────────────────────────────────────

    /**
     * Core test logic shared by both TC-01 (iPhone) and TC-02 (Galaxy).
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Open Amazon.com.</li>
     *   <li>Search for {@code searchQuery}.</li>
     *   <li>Click the first organic result.</li>
     *   <li>Record the product title and price from the detail page.</li>
     *   <li>Add the product to the cart.</li>
     *   <li>Verify the cart is not empty.</li>
     *   <li>Print the price to the console.</li>
     * </ol>
     *
     * @param searchQuery  e.g. "Apple iPhone 15"
     * @param productLabel e.g. "iPhone"
     */
    @Test(description = "Search → Select → Add to Cart → Verify & Print Price")
    @Parameters({"searchQuery", "productLabel"})
    public void searchAddToCartAndVerifyPrice(String searchQuery, String productLabel) {

        WebDriver driver = DriverConfig.getDriver();

        // ── Step 1: Open Amazon ───────────────────────────────────────────
        log.info("[Thread-{}] STEP 1 – Opening Amazon.com", threadId());
        ExtentReportManager.getTest().info("Opening amazon.com…");

        HomePage homePage = new HomePage(driver);
        homePage.open();
        ExtentReportManager.getTest().pass("Amazon home page loaded.");

        // ── Step 2: Search ────────────────────────────────────────────────
        log.info("[Thread-{}] STEP 2 – Searching for '{}'", threadId(), searchQuery);
        ExtentReportManager.getTest().info("Searching for: <b>" + searchQuery + "</b>");

        SearchResultsPage resultsPage = homePage.searchFor(searchQuery);
        ExtentReportManager.getTest().pass("Search results page loaded.");

        // ── Step 3: Select first result ───────────────────────────────────
        log.info("[Thread-{}] STEP 3 – Selecting first result", threadId());
        ExtentReportManager.getTest().info("Selecting first organic result…");

        ProductPage productPage = resultsPage.clickFirstResult();

        String productTitle = productPage.getProductTitle();
        log.info("[Thread-{}] Product selected: '{}'", threadId(), productTitle);
        ExtentReportManager.getTest()
                .pass("Product page loaded: <b>" + productTitle + "</b>");

        // ── Step 4: Read price ────────────────────────────────────────────
        log.info("[Thread-{}] STEP 4 – Reading price", threadId());

        String price = productPage.getPrice();

        // ════════════════════════════════════════════════════════════════════
        //  PRICE OUTPUT  (the primary verification requirement)
        // ════════════════════════════════════════════════════════════════════
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.printf ("║  %-52s║%n", " Product  : " + trim(productTitle, 40));
        System.out.printf ("║  %-52s║%n", " Query    : " + searchQuery);
        System.out.printf ("║  %-52s║%n", " Price    : " + price);
        System.out.printf ("║  %-52s║%n", " Thread   : " + threadId());
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        log.info("[Thread-{}] ★ PRICE RETRIEVED: {} → {}", threadId(), productLabel, price);
        ExtentReportManager.getTest()
                .info("Price retrieved: <b>" + price + "</b>");

        // ── Step 5: Add to Cart ───────────────────────────────────────────
        log.info("[Thread-{}] STEP 5 – Adding to cart", threadId());
        ExtentReportManager.getTest().info("Adding product to cart…");

        CartPage cartPage = productPage.addToCart();
        cartPage.open();   // navigate to cart page for explicit verification

        // ── Step 6: Verify cart ───────────────────────────────────────────
        log.info("[Thread-{}] STEP 6 – Verifying cart is not empty", threadId());

        boolean cartHasItems = cartPage.hasItems();
        String  cartTitle    = cartPage.getFirstItemTitle();
        String  cartPrice    = cartPage.getFirstItemPrice();

        log.info("[Thread-{}] Cart title  : {}", threadId(), cartTitle);
        log.info("[Thread-{}] Cart price  : {}", threadId(), cartPrice);
        log.info("[Thread-{}] Cart items  : {}", threadId(), cartPage.getItemCount());

        ExtentReportManager.getTest()
                .info("Cart item: <b>" + cartTitle + "</b> @ <b>" + cartPrice + "</b>");

        Assert.assertTrue(cartHasItems,
                "[" + productLabel + "] Cart should contain at least one item after Add to Cart.");

        ExtentReportManager.getTest()
                .pass("✓ Cart verified – item present. Price = " + price);

        log.info("[Thread-{}] ✓ TEST PASSED  [{}]  Price = {}",
                threadId(), productLabel, price);
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private long threadId() {
        return Thread.currentThread().getId();
    }

    /** Truncate a string to maxLen characters to keep console table tidy. */
    private String trim(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
