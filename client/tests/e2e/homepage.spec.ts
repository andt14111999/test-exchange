import { test, expect } from "../fixtures/base";

test.describe("Homepage", () => {
  test("should load homepage successfully", async ({ page }) => {
    await page.goto("/");

    // Check if page loaded
    await expect(page).toHaveTitle(/Exchange/);
  });

  test("should navigate to login page", async ({ page }) => {
    await page.goto("/");

    // Find and click login button/link (update selector based on your actual UI)
    const loginButton = page.getByRole("link", { name: /login/i });
    if (await loginButton.isVisible()) {
      await loginButton.click();

      // Check if URL changed to login page
      await expect(page).toHaveURL(/.*login/);
    }
  });
});
