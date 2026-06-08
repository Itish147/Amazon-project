package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SearchResultsPage
 * ════════════════════════════════════════════════════════════════════════════
 * Represents the Amazon search-results listing page (s?k=...).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Wait for result cards to appear after a search.</li>
 *   <li>Select the first non-sponsored organic result.</li>
 *   <li>Return a {@link ProductPage} after clicking the chosen product.</li>
 * </ul>
 */
public class SearchResultsPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(SearchResultsPage.class);

    // ── Selectors ─────────────────────────────────────────────────────────

    /** Container for every result card on the page */
    private static final By RESULT_CARDS    =
            By.cssSelector("div[data-component-type='s-search-result']");

    /** Sponsored label inside a card */
    private static final By SPONSORED_LABEL =
            By.cssSelector("span.s-label-popover-default, "
                           + "span[aria-label='Sponsored'], "
                           + "span.puis-sponsored-label-text");

    /** Product title link inside a result card */
    private static final By PRODUCT_LINK    =
            By.cssSelector("h2 a.a-link-normal");

    /** Title text span inside the link */
    private static final By TITLE_SPAN      =
            By.cssSelector("h2 a span");

    public SearchResultsPage(WebDriver driver) {
        super(driver);
        // Wait for at least one result card before continuing
        waitVisible(RESULT_CARDS, longWait);
        log.info("[Thread-{}] Search results page loaded.", Thread.currentThread().getId());
    }

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Click the first organic (non-sponsored) product in the results list.
     * Falls back to the very first card if every visible card is sponsored.
     *
     * @return a new {@link ProductPage} after navigation
     */
    public ProductPage clickFirstResult() {
        List<WebElement> cards = driver.findElements(RESULT_CARDS);
        log.info("[Thread-{}] Found {} result cards.", Thread.currentThread().getId(), cards.size());

        WebElement chosenCard  = cards.get(0);   // safe default
        String     chosenTitle = "(unknown)";
        int        chosenIndex = 0;

        // Prefer first non-sponsored card (check up to the first 8 results)
        for (int i = 0; i < Math.min(cards.size(), 8); i++) {
            WebElement card = cards.get(i);
            if (!card.findElements(SPONSORED_LABEL).isEmpty()) {
                log.debug("[Thread-{}] Card[{}] is sponsored – skipping.",
                        Thread.currentThread().getId(), i);
                continue;
            }
            chosenCard  = card;
            chosenIndex = i;
            List<WebElement> titleEls = card.findElements(TITLE_SPAN);
            if (!titleEls.isEmpty()) {
                chosenTitle = titleEls.get(0).getText().trim();
            }
            break;
        }

        log.info("[Thread-{}] Selecting card[{}]: '{}'",
                Thread.currentThread().getId(), chosenIndex, chosenTitle);

        // Scroll the card into view then click its title link
        List<WebElement> links = chosenCard.findElements(PRODUCT_LINK);
        if (!links.isEmpty()) {
            scrollIntoView(links.get(0));
            links.get(0).click();
        } else {
            // Fallback: JS-click the card itself
            jsClick(chosenCard);
        }

        return new ProductPage(driver);
    }
}
