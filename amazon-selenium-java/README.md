# Amazon Selenium Automation Suite

> **Java 11 · Selenium 4 · TestNG · Parallel Execution · ExtentReports**

A production-grade test automation suite that searches Amazon for an **iPhone** and a **Samsung Galaxy** device, adds each to the shopping cart, and **prints the product price to the console** — with both test cases executing **in parallel** using TestNG's native threading model.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running the Tests](#running-the-tests)
- [Parallel Execution Explained](#parallel-execution-explained)
- [Configuration](#configuration)
- [LambdaTest Cloud Execution (Bonus)](#lambdatest-cloud-execution-bonus)
- [Reports & Screenshots](#reports--screenshots)
- [Test Cases](#test-cases)
- [Design Decisions](#design-decisions)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                  TestNG Parallel Suite                   │
│         (testng-parallel.xml, thread-count=2)            │
│                                                          │
│  Thread 1 (TC-01)              Thread 2 (TC-02)          │
│  ─────────────────              ─────────────────        │
│  searchQuery=iPhone             searchQuery=Galaxy       │
│        │                               │                 │
│        ▼                               ▼                 │
│  DriverConfig.ThreadLocal       DriverConfig.ThreadLocal │
│  (independent ChromeDriver)     (independent ChromeDriver│
│        │                               │                 │
│        ▼                               ▼                 │
│   HomePage ──► SearchResultsPage ──► ProductPage         │
│                                       │                  │
│                                  getPrice()              │
│                                  addToCart()             │
│                                       │                  │
│                                   CartPage               │
│                                  (verify + print)        │
└──────────────────────────────────────────────────────────┘
```

**Key principle:** `ThreadLocal<WebDriver>` in `DriverConfig` guarantees each thread owns its own isolated browser session — zero race conditions.

---

## Project Structure

```
amazon-selenium-java/
├── pom.xml                                  # Maven build + dependencies
├── README.md
│
├── src/test/
│   ├── resources/
│   │   ├── testng-parallel.xml              # ← Parallel suite definition
│   │   └── logback-test.xml                 # Logging configuration
│   │
│   └── java/com/amazon/
│       ├── config/
│       │   ├── DriverConfig.java            # ThreadLocal WebDriver factory
│       │   └── LambdaTestConfig.java        # Cloud execution (Bonus)
│       │
│       ├── pages/                           # Page Object Model
│       │   ├── BasePage.java                # Shared Selenium helpers
│       │   ├── HomePage.java                # amazon.com landing page
│       │   ├── SearchResultsPage.java       # Search results listing
│       │   ├── ProductPage.java             # Product detail + price
│       │   └── CartPage.java                # Shopping cart
│       │
│       ├── tests/
│       │   └── ProductSearchCartTest.java   # TC-01 & TC-02 (parameterised)
│       │
│       └── utils/
│           ├── ExtentReportManager.java     # Thread-safe HTML reports
│           └── ScreenshotUtil.java          # Failure screenshots
│
├── reports/                                 # Generated after run
└── screenshots/                             # Failure screenshots
```

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| **Java JDK** | 11+ | `java -version` to verify |
| **Maven** | 3.8+ | `mvn -version` to verify |
| **Google Chrome** | Latest | WebDriverManager auto-downloads matching ChromeDriver |
| **Internet access** | — | Required to reach amazon.com |

---

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/amazon-selenium-java.git
cd amazon-selenium-java

# 2. Install dependencies (no manual ChromeDriver download needed)
mvn dependency:resolve

# 3. Run the full parallel suite
mvn test
```

That's it. Maven compiles everything, WebDriverManager downloads the correct ChromeDriver, and both test cases run in parallel.

---

## Running the Tests

### Run the full parallel suite (default)
```bash
mvn test
```

### Run with visible browser windows (non-headless)
```bash
mvn test -Dheadless=false
```

### Run a single test case directly
```bash
# TC-01 only
mvn test -Dtest="ProductSearchCartTest#searchAddToCartAndVerifyPrice" \
         -DsearchQuery="Apple iPhone 15" -DproductLabel="iPhone"
```

### Run on LambdaTest cloud
```bash
export LT_USERNAME=your_username
export LT_ACCESS_KEY=your_access_key
mvn test -DuseLambdaTest=true
```

---

## Parallel Execution Explained

The suite uses **TestNG's `parallel="tests"`** mode. Here is how it works:

```xml
<!-- testng-parallel.xml -->
<suite name="Amazon Automation Suite"
       parallel="tests"
       thread-count="2">

    <test name="TC-01 | iPhone Search and Cart">
        <parameter name="searchQuery"  value="Apple iPhone 15"/>
        <parameter name="productLabel" value="iPhone"/>
        <classes>
            <class name="com.amazon.tests.ProductSearchCartTest"/>
        </classes>
    </test>

    <test name="TC-02 | Galaxy Device Search and Cart">
        <parameter name="searchQuery"  value="Samsung Galaxy S24"/>
        <parameter name="productLabel" value="Galaxy"/>
        <classes>
            <class name="com.amazon.tests.ProductSearchCartTest"/>
        </classes>
    </test>

</suite>
```

- `parallel="tests"` → each `<test>` block runs in its own thread simultaneously
- `thread-count="2"` → two threads execute concurrently
- **`ThreadLocal<WebDriver>`** in `DriverConfig` → each thread gets its own Chrome instance, preventing any shared state

```
Timeline:

t=0s   Thread-1 opens Chrome, navigates to Amazon (iPhone search)
t=0s   Thread-2 opens Chrome, navigates to Amazon (Galaxy search)  ← simultaneous
t=15s  Thread-1 adds iPhone to cart, prints price
t=16s  Thread-2 adds Galaxy to cart, prints price
t=17s  Both threads close their browsers
       Total wall-clock time ≈ 17s  (vs ~34s sequential)
```

---

## Configuration

| System Property | Default | Description |
|---|---|---|
| `headless` | `false` | Run Chrome without a GUI (`true` for CI) |
| `useLambdaTest` | `false` | Route through LambdaTest cloud grid |

### Environment Variables (LambdaTest only)

| Variable | Description |
|---|---|
| `LT_USERNAME` | LambdaTest account username |
| `LT_ACCESS_KEY` | LambdaTest access key (from Account → Security) |

---

## LambdaTest Cloud Execution (Bonus)

[LambdaTest](https://www.lambdatest.com) is a cloud-based Selenium grid that lets you run tests on real browsers without maintaining local infrastructure.

### Setup
1. [Sign up](https://accounts.lambdatest.com/register) for a free account
2. Find your credentials at **Account → Security**
3. Export them as environment variables:
   ```bash
   export LT_USERNAME=your_username
   export LT_ACCESS_KEY=your_access_key
   ```
4. Run:
   ```bash
   mvn test -DuseLambdaTest=true
   ```

The `LambdaTestConfig` class builds a `RemoteWebDriver` with these capabilities:
- **Browser:** Chrome Latest
- **Platform:** Windows 11
- **Video recording, console logs, and network logs** all enabled

Both TC-01 and TC-02 run in parallel on the LambdaTest cloud, each as a separate session visible in your LambdaTest dashboard.

---

## Reports & Screenshots

### HTML Report (ExtentReports)
After every run, an interactive HTML report is saved to:
```
reports/ExtentReport_<timestamp>.html
```
Open it in any browser. It shows pass/fail status, step-level logs, timestamps, and system info for each test.

### Console Output
Both test cases print a formatted price summary to stdout:
```
╔══════════════════════════════════════════════════════╗
║   Product  : Apple iPhone 15 (128 GB) – Black       ║
║   Query    : Apple iPhone 15                         ║
║   Price    : $699.00                                 ║
║   Thread   : 21                                      ║
╚══════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════╗
║   Product  : Samsung Galaxy S24 (256 GB)            ║
║   Query    : Samsung Galaxy S24                      ║
║   Price    : $799.99                                 ║
║   Thread   : 22                                      ║
╚══════════════════════════════════════════════════════╝
```

### Failure Screenshots
If a test fails, a full-page PNG screenshot is automatically saved to:
```
screenshots/FAIL_<testName>_<timestamp>.png
```

---

## Test Cases

### TC-01 — iPhone Search and Cart
| Step | Action | Verification |
|---|---|---|
| 1 | Navigate to amazon.com | Page title contains "Amazon" |
| 2 | Search for "Apple iPhone 15" | Results page loads |
| 3 | Click first organic result | Product detail page loads |
| 4 | Read price | Price printed to console |
| 5 | Click "Add to Cart" | Cart confirmation received |
| 6 | Open cart | Item is present in cart |

### TC-02 — Galaxy Device Search and Cart
Identical flow with `searchQuery = "Samsung Galaxy S24"`. Runs concurrently with TC-01 on a separate thread and browser instance.

---

## Design Decisions

| Decision | Rationale |
|---|---|
| **Page Object Model** | Separates locators/actions from test logic; changes to Amazon's HTML only require updates in one place |
| `ThreadLocal<WebDriver>` | Industry-standard pattern for thread-safe parallel WebDriver management |
| `parallel="tests"` over `parallel="methods"` | Cleanest isolation — each test case gets its own browser, avoiding shared state |
| **WebDriverManager** | Eliminates manual ChromeDriver version management; auto-matches the installed Chrome version |
| **Multiple price selectors** | Amazon renders prices differently across product types; a fallback chain is more robust than a single selector |
| **ExtentReports** | Rich, shareable HTML reports that are more useful than raw Surefire XML for QA review |

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| selenium-java | 4.21.0 | Browser automation |
| testng | 7.10.2 | Test framework + parallel execution |
| webdrivermanager | 5.8.0 | Automatic driver binary management |
| extentreports | 5.1.1 | HTML test reporting |
| lombok | 1.18.32 | Boilerplate reduction |
| logback-classic | 1.5.6 | Structured console + file logging |

---

*Built with Java 11 · Selenium 4 · TestNG · Maven*
