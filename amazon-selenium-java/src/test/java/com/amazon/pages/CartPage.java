package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CartPage
 * ════════════════════════════════════════════════════════════════════════════
 * Represents the Amazon shopping cart (cart/view page).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Navigate to the cart directly if not already there.</li>
 *   <li>Verify the expected product is present.</li>
 *   <li>Return the cart-level price for the item.</li>
 * </ul>
 */
public class CartPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(CartPage.class);

    // ── Selectors ─────────────────────────────────────────────────────────

    /** Cart icon / count in the nav bar */
    private static final By CART_ICON =
            By.id("nav-cart");

    /** Each item row in the cart */
    private static final By CART_ITEMS =
            By.cssSelector("#sc-active-cart .sc-list-item, "
                           + "div[data-itemtype='active'] .sc-list-item");

    /** Product name within a cart row */
    private static final By CART_ITEM_TITLE =
            By.cssSelector("span.sc-product-title, "
                           + ".a-truncate-full.sc-product-title");

    /** Price within a cart row */
    private static final By[] CART_PRICE_SELECTORS = {
        By.cssSelector(".sc-product-price .a-offscreen"),
        By.cssSelector("span.a-price .a-offscreen"),
        By.cssSelector(".sc-price"),
    };

    /** Empty-cart indicator */
    private static final By EMPTY_CART_MSG =
            By.cssSelector("#sc-active-cart .sc-your-amazon-cart-is-empty, "
                           + "h2[data-ux='EmptyCartTitle']");

    private static final String CART_URL = "https://www.amazon.com/gp/cart/view.html";

    public CartPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ────────────────────────────────────────────────────────

    /**
     * Navigate directly to the cart page (useful if the confirmation
     * overlay was dismissed before we could act on it).
     *
     * @return this page (fluent)
     */
    public CartPage open() {
        navigateTo(CART_URL);
        log.info("[Thread-{}] Cart page opened.", Thread.currentThread().getId());
        return this;
    }

    // ── Verification helpers ──────────────────────────────────────────────

    /**
     * Return {@code true} if the cart contains at least one item.
     */
    public boolean hasItems() {
        if (isPresent(EMPTY_CART_MSG)) return false;
        return !driver.findElements(CART_ITEMS).isEmpty();
    }

    /**
     * Return the number of distinct line-items currently in the cart.
     */
    public int getItemCount() {
        return driver.findElements(CART_ITEMS).size();
    }

    /**
     * Return the title of the first cart item, or an empty string if the
     * cart is empty.
     */
    public String getFirstItemTitle() {
        List<WebElement> titles = driver.findElements(CART_ITEM_TITLE);
        if (titles.isEmpty()) return "";
        return titles.get(0).getText().trim();
    }

    /**
     * Retrieve the displayed price of the first cart item.
     * Tries multiple selectors because Amazon's cart HTML varies by
     * whether a coupon, warehouse deal, or subscribe-and-save is active.
     *
     * @return price string (e.g. "$999.00") or "Price not found"
     */
    public String getFirstItemPrice() {
        List<WebElement> rows = driver.findElements(CART_ITEMS);
        if (rows.isEmpty()) return "Cart is empty";

        WebElement firstRow = rows.get(0);
        for (By selector : CART_PRICE_SELECTORS) {
            List<WebElement> priceEls = firstRow.findElements(selector);
            if (!priceEls.isEmpty()) {
                String val = priceEls.get(0).getAttribute("textContent");
                if (val == null) val = priceEls.get(0).getText();
                val = val.trim();
                if (!val.isEmpty()) {
                    log.debug("[Thread-{}] Cart price via {}: {}",
                            Thread.currentThread().getId(), selector, val);
                    return val;
                }
            }
        }
        return "Price not found";
    }
}
