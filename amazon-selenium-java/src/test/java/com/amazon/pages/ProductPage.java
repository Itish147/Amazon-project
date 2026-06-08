package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ProductPage
 * ════════════════════════════════════════════════════════════════════════════
 * Represents an Amazon Product Detail Page (PDP).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Extract the product title and price.</li>
 *   <li>Add the product to the cart.</li>
 *   <li>Return a {@link CartPage} after the add-to-cart action.</li>
 * </ul>
 */
public class ProductPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(ProductPage.class);

    // ── Selectors ─────────────────────────────────────────────────────────

    /** Main product title */
    private static final By PRODUCT_TITLE =
            By.id("productTitle");

    /**
     * Price selectors listed in priority order.
     * Amazon renders prices differently depending on whether the product
     * has variants, a deal price, or a "used & new" section.
     */
    private static final By[] PRICE_SELECTORS = {
        By.cssSelector("span.a-price.aok-align-center span.a-offscreen"),   // deal / primary
        By.cssSelector("#corePriceDisplay_desktop_feature_div "
                + ".a-price .a-offscreen"),                                 // core price block
        By.cssSelector("#corePrice_desktop span.a-offscreen"),              // alternate
        By.cssSelector("span.a-price .a-offscreen"),                        // generic fallback
        By.id("priceblock_ourprice"),                                       // legacy
        By.id("priceblock_dealprice"),                                      // deal
        By.cssSelector("#apex_desktop_newAccordionRow span.a-offscreen"),   // accordion
    };

    /** "Add to Cart" button */
    private static final By ADD_TO_CART_BTN =
            By.id("add-to-cart-button");

    /** Confirmation banner / overlay after adding to cart */
    private static final By CART_CONFIRMATION =
            By.cssSelector(
                "#NATC_SMART_WAGON_CONF_MSG_SUCCESS, "
                + "#sw-atc-confirmation, "
                + "#huc-v2-order-row-confirm-text, "
                + "span[class*='sw-atc-asin-added']"
            );

    public ProductPage(WebDriver driver) {
        super(driver);
        // Wait for the title to confirm we are on a product page
        waitVisible(PRODUCT_TITLE, longWait);
        log.info("[Thread-{}] Product page loaded: {}",
                Thread.currentThread().getId(), driver.getTitle());
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /**
     * Return the trimmed product title.
     */
    public String getProductTitle() {
        return getText(PRODUCT_TITLE);
    }

    /**
     * Attempt to extract the product price by trying each selector in order.
     * Returns {@code "Price not found"} if none of the selectors match.
     */
    public String getPrice() {
        for (By selector : PRICE_SELECTORS) {
            List<WebElement> els = driver.findElements(selector);
            if (!els.isEmpty()) {
                String text = els.get(0).getAttribute("textContent");
                if (text == null) text = els.get(0).getText();
                text = text.trim();
                if (!text.isEmpty() && text.contains("$")) {
                    log.debug("[Thread-{}] Price found via selector {}: {}",
                            Thread.currentThread().getId(), selector, text);
                    return text;
                }
            }
        }
        log.warn("[Thread-{}] Could not locate price on page: {}",
                Thread.currentThread().getId(), driver.getCurrentUrl());
        return "Price not found";
    }

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Click "Add to Cart" and wait for the confirmation signal.
     *
     * @return a new {@link CartPage}
     */
    public CartPage addToCart() {
        log.info("[Thread-{}] Clicking 'Add to Cart'.", Thread.currentThread().getId());

        WebElement btn = waitClickable(ADD_TO_CART_BTN);
        scrollIntoView(btn);
        btn.click();

        // Wait for any confirmation element to appear (best-effort)
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(CART_CONFIRMATION));
            log.info("[Thread-{}] Add-to-cart confirmation received.",
                    Thread.currentThread().getId());
        } catch (Exception e) {
            log.warn("[Thread-{}] Confirmation banner not detected; proceeding anyway.",
                    Thread.currentThread().getId());
        }

        return new CartPage(driver);
    }
}
