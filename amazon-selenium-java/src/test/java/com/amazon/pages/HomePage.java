package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomePage
 * ════════════════════════════════════════════════════════════════════════════
 * Represents the Amazon.com landing page.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Open amazon.com</li>
 *   <li>Dismiss the sign-in / location pop-ups when they appear</li>
 *   <li>Trigger a product search and hand off to {@link SearchResultsPage}</li>
 * </ul>
 */
public class HomePage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(HomePage.class);

    // ── Selectors ─────────────────────────────────────────────────────────
    private static final By SEARCH_BOX    = By.id("twotabsearchtextbox");
    private static final By SEARCH_BUTTON = By.id("nav-search-submit-button");
    private static final By DISMISS_POPUP = By.cssSelector(
            "span[data-action='a-popover-close'], "
            + "button[data-action='a-popover-close'], "
            + "#nav-flyout-anchor-close");

    private static final String AMAZON_URL = "https://www.amazon.com";

    public HomePage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Navigate to Amazon and attempt to dismiss any overlay popups.
     *
     * @return this page (fluent API)
     */
    public HomePage open() {
        navigateTo(AMAZON_URL);
        dismissPopupsIfPresent();
        log.info("[Thread-{}] Amazon home page opened.", Thread.currentThread().getId());
        return this;
    }

    /**
     * Type the search query and press Enter (or click the search button).
     *
     * @param query product search term
     * @return a new {@link SearchResultsPage}
     */
    public SearchResultsPage searchFor(String query) {
        log.info("[Thread-{}] Searching for: '{}'", Thread.currentThread().getId(), query);
        type(SEARCH_BOX, query);
        waitClickable(SEARCH_BUTTON).sendKeys(Keys.ENTER);
        return new SearchResultsPage(driver);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Silently close any sign-in, location, or cookie popups.
     * Does nothing if no popup is found.
     */
    private void dismissPopupsIfPresent() {
        if (isPresent(DISMISS_POPUP)) {
            try {
                click(DISMISS_POPUP);
                log.debug("[Thread-{}] Popup dismissed.", Thread.currentThread().getId());
            } catch (Exception ignored) {
                // Popup disappeared before we could click – that's fine.
            }
        }
    }
}
