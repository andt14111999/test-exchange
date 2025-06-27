import { jest } from "@jest/globals";
import { getDeviceType, getDeviceInfo } from "@/lib/utils/device";

// Mock ua-parser-js
const mockGetResult = jest.fn();
jest.mock("ua-parser-js", () => ({
  UAParser: jest.fn().mockImplementation(() => ({
    getResult: mockGetResult,
  })),
}));

describe("Device Utils", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetResult.mockReturnValue({
      browser: { name: "Chrome" },
      os: { name: "Mac OS" },
      device: { model: null },
    });
  });

  describe("getDeviceType", () => {
    it("should return ios for iPhone", () => {
      Object.defineProperty(global, "navigator", {
        value: { userAgent: "iPhone" },
        writable: true,
        configurable: true,
      });

      const result = getDeviceType();
      expect(result).toBe("ios");
    });

    it("should return ios for iPad", () => {
      Object.defineProperty(global, "navigator", {
        value: { userAgent: "iPad" },
        writable: true,
        configurable: true,
      });

      const result = getDeviceType();
      expect(result).toBe("ios");
    });

    it("should return android for Android devices", () => {
      Object.defineProperty(global, "navigator", {
        value: { userAgent: "Android" },
        writable: true,
        configurable: true,
      });

      const result = getDeviceType();
      expect(result).toBe("android");
    });

    it("should return web for other devices", () => {
      Object.defineProperty(global, "navigator", {
        value: { userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" },
        writable: true,
        configurable: true,
      });

      const result = getDeviceType();
      expect(result).toBe("web");
    });

    it("should return web when window is undefined", () => {
      const originalWindow = global.window;
      // @ts-ignore - Testing SSR environment
      delete global.window;

      const result = getDeviceType();
      expect(result).toBe("web");

      // Restore window
      global.window = originalWindow;
    });
  });

  describe("getDeviceInfo", () => {
    it("should return device info from UAParser", () => {
      Object.defineProperty(global, "navigator", {
        value: {
          userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
          platform: "MacIntel",
        },
        writable: true,
        configurable: true,
      });

      const result = getDeviceInfo();

      // Just check that it returns an object with the required properties
      expect(result).toHaveProperty("browser");
      expect(result).toHaveProperty("os");
      expect(result).toHaveProperty("device");
      expect(result).toHaveProperty("platform");
      expect(result.platform).toBe("MacIntel");
    });

    it("should return unknown values when window is undefined", () => {
      const originalWindow = global.window;
      // @ts-ignore - Testing SSR environment
      delete global.window;

      const result = getDeviceInfo();

      expect(result).toEqual({
        browser: "Unknown",
        os: "Unknown",
        device: "Unknown",
      });

      // Restore window
      global.window = originalWindow;
    });

    it("should handle missing UAParser results", () => {
      mockGetResult.mockReturnValue({
        browser: {},
        os: {},
        device: {},
      });

      Object.defineProperty(global, "navigator", {
        value: {
          userAgent: "test",
          // @ts-ignore - Testing undefined platform
          platform: undefined,
        },
        writable: true,
        configurable: true,
      });

      const result = getDeviceInfo();

      expect(result).toEqual({
        browser: "Unknown",
        os: "Unknown",
        device: "Desktop",
        platform: "Unknown",
      });
    });
  });
});
