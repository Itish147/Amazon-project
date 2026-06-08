package com.amazon.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * BasePage
 * ════════════════════════════════════════════════════════════════════════════
 * Abstract foundation for all Page Object classes.
 *
 * <p>Encapsulates common Selenium operations (click, type, getText, waitFor)
 * behind clean, reusable methods so individual page classes stay focused on
 * <em>what</em> a page does rather than <em>how</em> Selenium works.</p>
 *
 * <p>Uses {@link WebDriverWait} with explicit waits – never {@code Thread.sleep()}.</p>
 */
public abstract class BasePage {

    protected final WebDriver      driver;
    protected final WebDriverWait  wait;
    protected final WebDriverWait  longWait;

    private static final Logger log = LoggerFactory.getLogger(BasePage.class);

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(15);
    private static final Duration LONG_WAIT    = Duration.ofSeconds(30);

    protected BasePage(WebDriver driver) {
        this.driver   = driver;
        this.wait     = new WebDriverWait(driver, DEFAULT_WAIT);
        this.longWait = new WebDriverWait(driver, LONG_WAIT);
        PageFactory.initElements(driver, this);
    }

    // ── Navigation ────────────────────────────────────────────────────────

    /**
     * Open a URL and wait for the page to finish loading.
     */
    protected void navigateTo(String url) {
        log.info("[Thread-{}] Navigating → {}", threadId(), url);
        driver.get(url);
    }

    // ── Waits ─────────────────────────────────────────────────────────────

    /**
     * Wait until the element identified by {@code by} is visible and return it.
     */
    protected WebElement waitVisible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected WebElement waitVisible(By by, WebDriverWait customWait) {
        return customWait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /**
     * Wait until the element is clickable and return it.
     */
    protected WebElement waitClickable(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    // ── Interactions ──────────────────────────────────────────────────────

    /**
     * Click an element after waiting for it to be clickable.
     */
    protected void click(By by) {
        log.debug("[Thread-{}] Clicking: {}", threadId(), by);
        waitClickable(by).click();
    }

    /**
     * Clear a field and type text into it.
     */
    protected void type(By by, String text) {
        log.debug("[Thread-{}] Typing '{}' into: {}", threadId(), text, by);
        WebElement el = waitVisible(by);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Return the trimmed inner text of the first matching element.
     * Returns an empty string if the element is not found.
     */
    protected String getText(By by) {
        try {
            return waitVisible(by).getText().trim();
        } catch (TimeoutException e) {
            log.warn("[Thread-{}] getText timed out for: {}", threadId(), by);
            return "";
        }
    }

    /**
     * Return the trimmed value of an attribute on the first matching element.
     */
    protected String getAttribute(By by, String attribute) {
        try {
            WebElement el = waitVisible(by);
            String val = el.getAttribute(attribute);
            return val != null ? val.trim() : "";
        } catch (TimeoutException e) {
            log.warn("[Thread-{}] getAttribute timed out for: {}", threadId(), by);
            return "";
        }
    }

    /**
     * Check whether an element is present in the DOM right now (no wait).
     */
    protected boolean isPresent(By by) {
        return !driver.findElements(by).isEmpty();
    }

    /**
     * Scroll an element into the viewport using JavaScript.
     */
    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /**
     * JavaScript click – useful when a regular click is intercepted.
     */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private long threadId() {
        return Thread.currentThread().getId();
    }
}
