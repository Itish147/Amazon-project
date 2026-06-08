package com.amazon.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DriverConfig
 * ════════════════════════════════════════════════════════════════════════════
 * Thread-safe WebDriver factory built around {@link ThreadLocal}.
 *
 * <p>Because TestNG runs TC-01 and TC-02 in separate threads, each thread
 * must own its own WebDriver instance – sharing a single driver across threads
 * causes race conditions and session conflicts.  {@code ThreadLocal<WebDriver>}
 * guarantees every thread gets its own isolated copy.</p>
 *
 * <p><strong>Usage pattern:</strong></p>
 * <pre>
 *   DriverConfig.initDriver("chrome");   // called in @BeforeMethod
 *   WebDriver driver = DriverConfig.getDriver();
 *   DriverConfig.quitDriver();           // called in @AfterMethod
 * </pre>
 */
public final class DriverConfig {

    private static final Logger log = LoggerFactory.getLogger(DriverConfig.class);

    /** One driver slot per thread – the key to safe parallel execution. */
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    // Prevent instantiation
    private DriverConfig() {}

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Initialise and store a new WebDriver for the calling thread.
     *
     * @param browser  "chrome" or "firefox" (case-insensitive).
     *                 Defaults to Chrome for any other value.
     * @param headless {@code true} to run without a visible window (CI mode).
     */
    public static void initDriver(String browser, boolean headless) {
        WebDriver driver;

        switch (browser.toLowerCase()) {

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions ffOpts = new FirefoxOptions();
                if (headless) ffOpts.addArguments("--headless");
                driver = new FirefoxDriver(ffOpts);
                log.info("[Thread-{}] Firefox WebDriver initialised (headless={})",
                        Thread.currentThread().getId(), headless);
                break;

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOpts = buildChromeOptions(headless);
                driver = new ChromeDriver(chromeOpts);
                log.info("[Thread-{}] Chrome WebDriver initialised (headless={})",
                        Thread.currentThread().getId(), headless);
                break;
        }

        driver.manage().window().maximize();
        DRIVER_THREAD_LOCAL.set(driver);
    }

    /**
     * Convenience overload – defaults to Chrome, non-headless.
     */
    public static void initDriver() {
        boolean headless = Boolean.parseBoolean(
                System.getProperty("headless", "false"));
        initDriver("chrome", headless);
    }

    /**
     * Return the WebDriver bound to the current thread.
     *
     * @throws IllegalStateException if {@link #initDriver} has not been called
     *                               on this thread yet.
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver on thread " + Thread.currentThread().getId()
                    + ". Call DriverConfig.initDriver() first.");
        }
        return driver;
    }

    /**
     * Directly inject an already-created {@link WebDriver} for the calling thread.
     * Used by {@code LambdaTestConfig} to supply a RemoteWebDriver.
     *
     * @param driver pre-configured driver instance
     */
    public static void setDriver(WebDriver driver) {
        DRIVER_THREAD_LOCAL.set(driver);
        log.info("[Thread-{}] External WebDriver injected into ThreadLocal.",
                Thread.currentThread().getId());
    }

    /**
     * Quit the driver and remove it from the ThreadLocal to prevent leaks.
     * Safe to call even if the driver was never initialised.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            driver.quit();
            DRIVER_THREAD_LOCAL.remove();
            log.info("[Thread-{}] WebDriver quit and removed from ThreadLocal.",
                    Thread.currentThread().getId());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions opts = new ChromeOptions();

        if (headless) {
            opts.addArguments("--headless=new");   // Chrome 112+ headless mode
        }

        // Stability / anti-bot flags
        opts.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-blink-features=AutomationControlled",
                "--disable-extensions",
                "--start-maximized",
                "--window-size=1920,1080"
        );

        // Mimic a real browser user-agent to reduce CAPTCHA triggers
        opts.addArguments(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36"
        );

        // Suppress "Chrome is being controlled by automated software" banner
        opts.setExperimentalOption("excludeSwitches",
                new String[]{"enable-automation"});
        opts.setExperimentalOption("useAutomationExtension", false);

        return opts;
    }
}
