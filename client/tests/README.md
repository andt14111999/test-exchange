# Testing

This directory contains all tests for the application:

- `unit/`: Unit tests using Jest
- `e2e/`: End-to-end tests using Playwright
- `components/`: Component tests using Playwright
- `fixtures/`: Shared fixtures for tests
- `utils/`: Utility functions for tests

## Unit Tests (Jest)

Unit tests are located in the `unit/` directory and are used to test individual components and functions in isolation.

To run unit tests:

```bash
pnpm test:unit
```

For watch mode:

```bash
pnpm test:watch
```

For coverage:

```bash
pnpm test:unit:coverage
```

## End-to-End Tests (Playwright)

End-to-end tests are located in the `e2e/` directory and test the application as a whole.

To run E2E tests:

```bash
pnpm test:e2e
```

For UI mode:

```bash
pnpm test:e2e:ui
```

For headed mode:

```bash
pnpm test:e2e:headed
```

For debug mode:

```bash
pnpm test:e2e:debug
```

## Component Tests (Playwright)

Component tests using Playwright are located in the `components/` directory.

To run component tests:

```bash
pnpm test:components
```

## Adding Tests

### Adding Unit Tests

Add new unit tests to the `unit/` directory, mirroring the structure of the `src/` directory. For example, tests for `src/components/ui/Button.tsx` should be in `tests/unit/components/ui/Button.test.tsx`.

### Adding E2E Tests

Add new E2E tests to the `e2e/` directory. Create new test files with the `.spec.ts` extension.

## Available Commands

- `pnpm test`: Run all tests
- `pnpm test:ui`: Open Playwright UI mode
- `pnpm test:headed`: Run tests in headed browser mode
- `pnpm test:debug`: Run tests in debug mode
- `pnpm test:e2e`: Run only E2E tests
- `pnpm test:components`: Run only component tests
- `pnpm report`: Show the test report
- `pnpm test:coverage`: Run tests with coverage and generate a report (Chromium only)
- `pnpm coverage:report`: Generate coverage report from existing coverage data
- `pnpm test:all`: Run tests on all browsers (without coverage)

## Authentication

Tests that require authentication use the following approaches:

1. **UI Login**: Using `loginViaUI()` from `utils/auth.ts`
2. **Auth State**: Using pre-saved authentication state

## Code Coverage

The test suite is configured to collect JavaScript code coverage during test execution. Here's how it works:

1. The coverage fixture in `fixtures/coverage.ts` automatically collects coverage for all tests
2. Coverage data is saved to `.nyc_output` directory
3. After tests complete, the coverage report is generated in `coverage/` directory
4. View the HTML report at `coverage/index.html`

**Note about Browser Support:**

- Only **Chromium** supports JS coverage collection. When running `test:coverage`, tests will only run in Chromium.
- To run tests in all browsers without coverage, use `pnpm test:all`.

To run tests with coverage:

```bash
pnpm test:coverage
```

## Updating Test Selectors

If the UI changes, update the selectors in the test files accordingly. Prefer using:

1. Role-based selectors: `page.getByRole('button', { name: /login/i })`
2. Text-based selectors: `page.getByText(/welcome/i)`
3. Label-based selectors: `page.getByLabel(/email/i)`

## CI/CD Integration

Tests run automatically in CI pipeline. Failed tests will generate screenshots and traces that can be viewed in the Playwright report.
