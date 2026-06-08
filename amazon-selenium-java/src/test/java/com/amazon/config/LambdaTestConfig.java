package com.amazon.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * LambdaTestConfig  (Bonus)
 * ════════════════════════════════════════════════════════════════════════════
 * Creates a {@link RemoteWebDriver} connected to the LambdaTest Selenium Grid.
 *
 * <h3>Setup</h3>
 * Export the following environment variables (or set them in a .env file):
 * <pre>
 *   LT_USERNAME   – your LambdaTest username
 *   LT_ACCESS_KEY – your LambdaTest access key
 * </pre>
 *
 * Then pass {@code -DuseLambdaTest=true} when invoking Maven:
 * <pre>
 *   mvn test -DuseLambdaTest=true
 * </pre>
 *
 * <h3>How it plugs in</h3>
 * {@link DriverConfig#initDriver()} checks {@code System.getProperty("useLambdaTest")}.
 * If {@code true}, it delegates to {@link #createDriver(String)} here instead
 * of launching a local browser.
 */
public final class LambdaTestConfig {

    private static final Logger log = LoggerFactory.getLogger(LambdaTestConfig.class);

    /** LambdaTest Selenium 4 hub URL */
    private static final String HUB_URL =
            "https://%s:%s@hub.lambdatest.com/wd/hub";

    private LambdaTestConfig() {}

    /**
     * Build and return a {@link RemoteWebDriver} connected to LambdaTest.
     *
     * @param testName  displayed in the LambdaTest dashboard
     * @return configured {@link WebDriver}
     */
    public static WebDriver createDriver(String testName) {
        String username  = System.getenv("LT_USERNAME");
        String accessKey = System.getenv("LT_ACCESS_KEY");

        if (username == null || username.isBlank()
                || accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                    "LambdaTest credentials not set. "
                    + "Export LT_USERNAME and LT_ACCESS_KEY environment variables.");
        }

        // ── LambdaTest capabilities ───────────────────────────────────────
        Map<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("username",     username);
        ltOptions.put("accessKey",    accessKey);
        ltOptions.put("build",        "Amazon Automation Suite – Parallel");
        ltOptions.put("name",         testName);
        ltOptions.put("platformName", "Windows 11");
        ltOptions.put("browserVersion", "latest");
        ltOptions.put("network",      true);
        ltOptions.put("video",        true);
        ltOptions.put("console",      true);
        ltOptions.put("visual",       true);         // step-by-step screenshots
        ltOptions.put("tunnel",       false);

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName", "Chrome");
        caps.setCapability("LT:Options", ltOptions);

        try {
            String hubUrl = String.format(HUB_URL, username, accessKey);
            log.info("[Thread-{}] Connecting to LambdaTest grid…",
                    Thread.currentThread().getId());
            return new RemoteWebDriver(new URL(hubUrl), caps);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid LambdaTest hub URL", e);
        }
    }
}
