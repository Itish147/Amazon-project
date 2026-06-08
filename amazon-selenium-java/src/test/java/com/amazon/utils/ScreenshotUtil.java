package com.amazon.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtil
 * ════════════════════════════════════════════════════════════════════════════
 * Utility class for capturing and persisting Selenium screenshots.
 *
 * <p>Saved to {@code screenshots/<testName>_<timestamp>.png}.</p>
 */
public final class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = "screenshots";

    private ScreenshotUtil() {}

    /**
     * Capture a screenshot and write it to the screenshots directory.
     *
     * @param driver   active WebDriver instance
     * @param testName used as part of the filename (spaces replaced with "_")
     * @return absolute path to the saved file, or empty string on failure
     */
    public static String capture(WebDriver driver, String testName) {
        try {
            // Ensure directory exists
            new File(SCREENSHOT_DIR).mkdirs();

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String safeName  = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String fileName  = safeName + "_" + timestamp + ".png";
            Path   dest      = Paths.get(SCREENSHOT_DIR, fileName);

            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(dest, bytes);

            String absPath = dest.toAbsolutePath().toString();
            log.info("[Thread-{}] Screenshot saved → {}",
                    Thread.currentThread().getId(), absPath);
            return absPath;

        } catch (IOException e) {
            log.error("[Thread-{}] Failed to save screenshot: {}",
                    Thread.currentThread().getId(), e.getMessage());
            return "";
        }
    }
}
