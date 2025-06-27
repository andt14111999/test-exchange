import { defineConfig, devices } from "@playwright/test";
import { fileURLToPath } from "url";
import { dirname, join } from "path";

// Get current file URL and convert to directory path
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Determine if running in CI
const isCI = !!process.env.CI;

/**
 * See https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: "./tests",
  testIgnore: ["**/unit/**", "**/__tests__/**"],
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: isCI,
  /* Retry on CI only */
  retries: isCI ? 2 : 0,
  /* Reporter to use */
  reporter: "html",
  /* Global setup and teardown */
  globalSetup: join(__dirname, "tests/global-setup.ts"),
  globalTeardown: join(__dirname, "tests/global-teardown.ts"),
  /* Shared settings for all the projects below */
  use: {
    /* Base URL to use in actions like `await page.goto('/')` */
    baseURL: process.env.BASE_URL || "http://localhost:3000",
    /* Collect trace when retrying the failed test */
    trace: "on-first-retry",
    /* Capture screenshot on failure */
    screenshot: "only-on-failure",
    /* Record video on failure */
    video: "on-first-retry",
  },
  /* Configure projects for major browsers */
  projects: [
    /* Chromium is the only browser that supports JS coverage */
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "authenticated",
      use: {
        ...devices["Desktop Chrome"],
        // Use the saved authentication state
        storageState: "./tests/.auth/user.json",
      },
      dependencies: ["chromium"],
    },
  ],
  /* Run your local dev server before starting the tests - only in non-CI environment */
  ...(isCI
    ? {}
    : {
        webServer: {
          command: "pnpm dev",
          url: "http://localhost:3000",
          reuseExistingServer: true,
        },
      }),
});
