// Learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom";
import { TextDecoder, TextEncoder } from "util";

// Store original console methods
const originalConsoleError = console.error;
const originalConsoleWarn = console.warn;
const originalConsoleLog = console.log;

// Mock console methods before all tests
beforeAll(() => {
  console.error = jest.fn();
  console.warn = jest.fn();
  console.log = jest.fn();
  // Uncomment the following line if you want to suppress logs as well
  // console.log = jest.fn();
});

// Restore console methods after all tests
afterAll(() => {
  console.error = originalConsoleError;
  console.warn = originalConsoleWarn;
  console.log = originalConsoleLog;
});

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn(),
    replace: jest.fn(),
    back: jest.fn(),
    prefetch: jest.fn(),
    pathname: "/",
    route: "/",
    query: {},
  })),
  usePathname: jest.fn(() => "/"),
  useSearchParams: jest.fn(() => ({
    get: jest.fn(),
    has: jest.fn(),
    getAll: jest.fn(),
    keys: jest.fn(),
    values: jest.fn(),
  })),
}));

// Polyfill for TextEncoder/TextDecoder which are required by some dependencies
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

// Mock localStorage and sessionStorage
Object.defineProperty(window, "localStorage", {
  value: {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  },
  writable: true,
});

Object.defineProperty(window, "sessionStorage", {
  value: {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  },
  writable: true,
});

// Mock matchMedia
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: jest.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock ResizeObserver for tests
global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
}));

// Mock scrollIntoView for Radix UI components in JSDOM
window.HTMLElement.prototype.scrollIntoView = jest.fn();

// Reset mocks after each test
afterEach(() => {
  jest.clearAllMocks();
});

// Mock getTradingFees API to avoid network errors in tests
jest.mock("@/lib/api/settings", () => ({
  ...jest.requireActual("@/lib/api/settings"),
  getTradingFees: jest.fn(() =>
    Promise.resolve({
      fee_ratios: {
        vnd: 0.001,
        php: 0.001,
        ngn: 0.001,
        default: 0.001,
      },
      fixed_fees: {
        vnd: 1000,
        php: 1,
        ngn: 100,
        default: 1,
      },
    })
  ),
}));
