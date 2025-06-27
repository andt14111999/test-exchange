import { test, expect } from "../fixtures/base";

test.describe("Profile Page (alternative test)", () => {
  // Mock user data
  const mockUser = {
    id: 123,
    email: "test@example.com",
    display_name: "Test User",
    role: "USER",
    avatar_url: null,
    username: "testuser",
    created_at: "2023-01-01T00:00:00.000Z",
    updated_at: "2023-01-01T00:00:00.000Z",
  };

  test.beforeEach(async ({ page }) => {
    // Set token to simulate authentication
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "mock-auth-token");
    });
  });

  test("should redirect to login when not authenticated", async ({ page }) => {
    // Clear auth state
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });

    // Try to access profile page
    await page.goto("/profile");

    // Should redirect to login
    await expect(page).toHaveURL(/.*login/);
  });

  test("should load and display user data", async ({ page }) => {
    // Mock the API response for /users/me endpoint
    await page.route("**/users/me", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mockUser),
      });
    });

    // Enable verbose browser console logging
    page.on("console", (msg) => {
      console.log(`Browser console: ${msg.type()}: ${msg.text()}`);
    });

    // Go to profile page with authentication set up
    await page.goto("/profile");

    // Wait for content to load (since profile page has a loading state)
    await page.waitForLoadState("networkidle");

    // Wait a bit longer to ensure React has time to update the UI with the data
    await page.waitForTimeout(1000);

    // Look for elements that should be visible when user data is loaded
    // Use more specific selector to avoid strict mode violation
    await expect(
      page.getByText("Email:", { exact: false }).first(),
    ).toBeVisible({ timeout: 10000 });

    // Check that we can find the user's name
    await expect(
      page.getByText(mockUser.display_name, { exact: true }),
    ).toBeVisible();

    // Check for role information
    await expect(page.getByText("Role:", { exact: false })).toBeVisible();

    // Check for username display
    await expect(page.getByText("Username:", { exact: false })).toBeVisible();
    await expect(
      page.getByText(mockUser.username, { exact: false }),
    ).toBeVisible();
  });

  test("should redirect to update username page when username is not set", async ({
    page,
  }) => {
    // Create user without username
    const userWithoutUsername = { ...mockUser, username: null };

    // Mock the API response for /users/me endpoint
    await page.route("**/users/me", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(userWithoutUsername),
      });
    });

    // Go to profile page with authentication set up
    await page.goto("/profile");

    // Wait for redirection
    await page.waitForURL(/.*profile\/update/);

    // Verify we're on the update username page
    await expect(
      page.getByText("Update Username", { exact: false }),
    ).toBeVisible();
    await expect(page.getByPlaceholder("Enter your username")).toBeVisible();
  });
});
