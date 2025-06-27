// Mock next-intl/navigation module
jest.mock("next-intl/navigation", () => {
  // Mock implementation of createNavigation
  const createNavigationMock = jest.fn(() => ({
    Link: "mocked-link",
    redirect: jest.fn(),
    usePathname: jest.fn(),
    useRouter: jest.fn(),
  }));

  return {
    createNavigation: createNavigationMock,
  };
});

// Mock config/i18n module
jest.mock("@/config/i18n", () => ({
  locales: ["en", "vi", "fil"],
}));

// Import after mocks
import { Link, redirect, usePathname, useRouter } from "@/navigation";

describe("navigation", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should export Link correctly", () => {
    expect(Link).toBe("mocked-link");
  });

  it("should export redirect as a function", () => {
    expect(typeof redirect).toBe("function");
  });

  it("should export usePathname as a function", () => {
    expect(typeof usePathname).toBe("function");
  });

  it("should export useRouter as a function", () => {
    expect(typeof useRouter).toBe("function");
  });
});
