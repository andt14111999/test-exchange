import { Page } from "@playwright/test";

/**
 * Logs in a user using the UI
 * @param page Playwright page object
 * @param email User email
 * @param password User password
 */
export async function loginViaUI(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  // Navigate to login page
  await page.goto("/login");

  // Fill in login form
  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/password/i).fill(password);

  // Click login button
  await page.getByRole("button", { name: /login/i }).click();

  // Wait for navigation to complete
  await page.waitForURL("**/dashboard");
}

/**
 * Sets authentication cookies to bypass login UI
 * This is faster than going through the UI login flow each time
 * @param page Playwright page object
 */
export async function setupAuthState(page: Page): Promise<void> {
  // Add your implementation for setting auth cookies/localStorage
  // This will depend on how your application handles authentication

  // Example (modify according to your auth implementation):
  await page.evaluate(() => {
    // Store auth token in localStorage or set cookies
    localStorage.setItem("authToken", "your-test-auth-token");
  });

  // Navigate to a page after setting the auth state
  await page.goto("/");
}

/**
 * Clears authentication state
 * @param page Playwright page object
 */
export async function clearAuthState(page: Page): Promise<void> {
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}
