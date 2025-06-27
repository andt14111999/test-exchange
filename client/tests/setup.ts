// Store original console methods
const originalConsoleError = console.error;
const originalConsoleWarn = console.warn;
const originalConsoleLog = console.log;

// Mock console methods before all tests
beforeAll(() => {
  console.error = jest.fn();
  console.warn = jest.fn();
  // Uncomment the following line if you want to suppress logs as well
  // console.log = jest.fn();
});

// Restore console methods after all tests
afterAll(() => {
  console.error = originalConsoleError;
  console.warn = originalConsoleWarn;
  console.log = originalConsoleLog;
});
