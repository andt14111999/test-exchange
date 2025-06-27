import nextJest from "next/jest.js";

const createJestConfig = nextJest({
  // Provide the path to your Next.js app to load next.config.js and .env files
  dir: "./",
});

// Add any custom config to be passed to Jest
/** @type {import('jest').Config} */
const config = {
  // Add more setup options before each test is run
  setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
  testEnvironment: "jest-environment-jsdom",
  preset: "ts-jest",
  testMatch: [
    "<rootDir>/tests/unit/**/*.[jt]s?(x)",
    "<rootDir>/tests/unit/?(*.)+(spec|test).[jt]s?(x)",
  ],
  testPathIgnorePatterns: [
    "<rootDir>/node_modules/",
    "<rootDir>/.next/",
    "<rootDir>/tests/e2e/", // Ignore E2E tests - those will use Playwright
    "<rootDir>/tests/components/", // Ignore component tests using Playwright
    "<rootDir>/tests/fixtures/", // Ignore Playwright fixtures
  ],
  transformIgnorePatterns: ["node_modules/(?!(next-intl|use-intl)/)"],
  moduleNameMapper: {
    // Handle CSS imports (with CSS modules)
    "^.+\\.module\\.(css|sass|scss)$": "identity-obj-proxy",
    // Handle CSS imports (without CSS modules)
    "^.+\\.(css|sass|scss)$": "<rootDir>/__mocks__/styleMock.js",
    // Handle image imports
    "^.+\\.(png|jpg|jpeg|gif|webp|avif|ico|bmp|svg)$":
      "<rootDir>/__mocks__/fileMock.js",
    // Handle module aliases
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  collectCoverageFrom: [
    "src/**/*.{js,jsx,ts,tsx}",
    "!src/**/*.d.ts",
    "!src/**/types.ts",
    "!src/**/*.stories.{js,jsx,ts,tsx}",
    "!**/node_modules/**",
  ],
  coverageDirectory: "coverage",
  // Add custom reporters for more detailed coverage reports if needed
  coverageReporters: ["text", "lcov", "html"],
  transform: {
    "^.+\\.(t|j)sx?$": [
      "@swc/jest",
      {
        jsc: {
          transform: {
            react: {
              runtime: "automatic",
            },
          },
        },
      },
    ],
  },
  // Limit the number of workers to reduce memory usage
  maxWorkers: "50%",
  // Add extra options to help with memory usage
  globals: {
    "ts-jest": {
      isolatedModules: true, // This can help with memory usage
    },
  },
  // Disable collecting coverage from Next.js special directories to reduce overhead
  coveragePathIgnorePatterns: [
    "<rootDir>/.next/",
    "<rootDir>/node_modules/",
    "<rootDir>/src/app/api/", // API routes typically don't need coverage
  ],
  // Explicitly tell Jest to ignore the .next directory to avoid Haste module naming collisions
  modulePathIgnorePatterns: ["<rootDir>/.next/"],
  // Configure how the Haste module mapping system behaves
  haste: {
    // Ignore package.json files that might cause collisions
    hasteImplModulePath: null,
    enableSymlinks: false,
    throwOnModuleCollision: false, // Don't throw on collisions
  },
};

// createJestConfig is exported this way to ensure that next/jest can load the Next.js config
export default createJestConfig(config);
