package com.amazon.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExtentReportManager
 * ════════════════════════════════════════════════════════════════════════════
 * Thread-safe singleton wrapper around ExtentReports 5.
 *
 * <p>Uses a {@link ThreadLocal} to keep each parallel test's {@link ExtentTest}
 * isolated so log entries are never mixed between threads.</p>
 *
 * <h3>Usage pattern</h3>
 * <pre>
 *   // In @BeforeSuite (once)
 *   ExtentReportManager.init();
 *
 *   // In @BeforeMethod (once per test thread)
 *   ExtentReportManager.createTest("TC-01 iPhone Search");
 *
 *   // In @Test body
 *   ExtentReportManager.getTest().info("Searching for iPhone…");
 *   ExtentReportManager.getTest().pass("Price printed successfully");
 *
 *   // In @AfterSuite (once)
 *   ExtentReportManager.flush();
 * </pre>
 */
public final class ExtentReportManager {

    private static final Logger log = LoggerFactory.getLogger(ExtentReportManager.class);

    /** Singleton ExtentReports instance, initialised once per suite. */
    private static ExtentReports extent;

    /** Per-thread test node – prevents log interleaving in parallel runs. */
    private static final ThreadLocal<ExtentTest> TEST_THREAD_LOCAL = new ThreadLocal<>();

    private ExtentReportManager() {}

    // ── Initialisation ────────────────────────────────────────────────────

    /**
     * Initialise the report. Call once from a {@code @BeforeSuite} method.
     */
    public static synchronized void init() {
        if (extent != null) return; // already initialised

        String timestamp  = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportPath = "reports/ExtentReport_" + timestamp + ".html";

        // Ensure the reports directory exists
        new File("reports").mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Amazon Automation Report");
        spark.config().setReportName("Amazon Product Search & Cart – Parallel Suite");
        spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Framework",    "Selenium 4 + TestNG");
        extent.setSystemInfo("Language",     "Java 11");
        extent.setSystemInfo("Parallelism",  "TestNG parallel='tests', thread-count=2");
        extent.setSystemInfo("Environment",  "Local / LambdaTest (optional)");

        log.info("ExtentReports initialised → {}", reportPath);
    }

    // ── Per-test API ──────────────────────────────────────────────────────

    /**
     * Create a new test node for the calling thread.
     *
     * @param testName display name in the report
     */
    public static void createTest(String testName) {
        ExtentTest test = extent.createTest(testName);
        TEST_THREAD_LOCAL.set(test);
    }

    /**
     * Return the {@link ExtentTest} for the calling thread.
     */
    public static ExtentTest getTest() {
        return TEST_THREAD_LOCAL.get();
    }

    /**
     * Remove the test reference from this thread's local storage.
     */
    public static void removeTest() {
        TEST_THREAD_LOCAL.remove();
    }

    // ── Flush ─────────────────────────────────────────────────────────────

    /**
     * Write all collected logs to the HTML file.
     * Call once from a {@code @AfterSuite} method.
     */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed to disk.");
        }
    }
}
