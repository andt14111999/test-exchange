import { FullConfig } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";

/**
 * Global setup function that runs once before all tests
 * This can be used for setting up test data, environment, etc.
 */
async function globalSetup(config: FullConfig): Promise<void> {
  // Example: Create test data or set up environment
  console.log("Starting test suite with config:", config.projects[0].name);

  // Create directories for coverage
  const coverageDir = path.join(process.cwd(), ".nyc_output");
  if (!fs.existsSync(coverageDir)) {
    fs.mkdirSync(coverageDir, { recursive: true });
  }

  // Authentication setup is temporarily disabled
  // When needed, uncomment the following code and update selectors:
  /*
  const { chromium } = await import('@playwright/test');
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  // Navigate to login page
  await page.goto(`${config.projects[0].use.baseURL}/login`);
  
  // Fill login form (update selectors based on your UI)
  await page.getByLabel(/email/i).fill('test@example.com');
  await page.getByLabel(/password/i).fill('Password123!');
  
  // Click login button
  await page.getByRole('button', { name: /login/i }).click();
  
  // Wait for navigation
  await page.waitForURL(/.*dashboard/);
  
  // Save auth state to reuse in tests
  await page.context().storageState({ path: './tests/.auth/user.json' });
  
  await browser.close();
  */
}

export default globalSetup;
