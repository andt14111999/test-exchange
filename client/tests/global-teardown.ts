import { FullConfig } from "@playwright/test";

/**
 * Global teardown function that runs once after all tests
 * This can be used for cleaning up test data, environment, etc.
 */
async function globalTeardown(config: FullConfig) {
  console.log("Completed test suite with config:", config.projects[0].name);

  // Clean up any resources created during tests
  // For example, delete test data or reset environment
}

export default globalTeardown;
