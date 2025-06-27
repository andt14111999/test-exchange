import {
  getDeviceTrustPreference,
  setDeviceTrustPreference,
  clearDeviceTrustPreference,
  shouldTrustDevice,
} from "@/lib/utils/device-trust-preference";

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

// Mock console.warn to avoid noise in tests
const mockConsoleWarn = jest.spyOn(console, "warn").mockImplementation();

describe("Device Trust Preference Utils", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockConsoleWarn.mockClear();
  });

  afterAll(() => {
    mockConsoleWarn.mockRestore();
  });

  describe("getDeviceTrustPreference", () => {
    it("should return false when no preference is stored", () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      const result = getDeviceTrustPreference();

      expect(result).toBe(false);
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith(
        "device_trust_preference",
      );
    });

    it("should return true for valid stored preference", () => {
      const currentDeviceUuid = "test-device-uuid";
      const preference = {
        trustDevice: true,
        deviceUuid: currentDeviceUuid,
        timestamp: Date.now(),
      };

      mockLocalStorage.getItem
        .mockReturnValueOnce(JSON.stringify(preference)) // First call for preference
        .mockReturnValueOnce(currentDeviceUuid); // Second call for device_uuid

      const result = getDeviceTrustPreference();

      expect(result).toBe(true);
    });

    it("should return false for different device UUID", () => {
      const preference = {
        trustDevice: true,
        deviceUuid: "old-device-uuid",
        timestamp: Date.now(),
      };

      mockLocalStorage.getItem
        .mockReturnValueOnce(JSON.stringify(preference))
        .mockReturnValueOnce("current-device-uuid");

      const result = getDeviceTrustPreference();

      expect(result).toBe(false);
    });

    it("should return false and clear expired preference", () => {
      const thirtyOneDaysAgo = Date.now() - 31 * 24 * 60 * 60 * 1000;
      const preference = {
        trustDevice: true,
        deviceUuid: "test-device-uuid",
        timestamp: thirtyOneDaysAgo,
      };

      mockLocalStorage.getItem
        .mockReturnValueOnce(JSON.stringify(preference))
        .mockReturnValueOnce("test-device-uuid");

      const result = getDeviceTrustPreference();

      expect(result).toBe(false);
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith(
        "device_trust_preference",
      );
    });

    it("should return false for invalid JSON", () => {
      mockLocalStorage.getItem.mockReturnValue("invalid-json");

      const result = getDeviceTrustPreference();

      expect(result).toBe(false);
    });

    it("should return false in SSR environment", () => {
      // Mock SSR environment by defining window as undefined
      const originalWindow = global.window;
      // @ts-ignore
      delete global.window;

      const result = getDeviceTrustPreference();

      expect(result).toBe(false);

      // Restore window
      global.window = originalWindow;
    });
  });

  describe("setDeviceTrustPreference", () => {
    it("should set preference with valid device UUID", () => {
      const deviceUuid = "test-device-uuid";
      mockLocalStorage.getItem.mockReturnValue(deviceUuid);

      const mockNow = 1234567890000;
      jest.spyOn(Date, "now").mockReturnValue(mockNow);

      setDeviceTrustPreference(true);

      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        "device_trust_preference",
        JSON.stringify({
          trustDevice: true,
          deviceUuid,
          timestamp: mockNow,
        }),
      );

      jest.restoreAllMocks();
    });

    it("should not set preference without device UUID", () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      setDeviceTrustPreference(true);

      expect(mockLocalStorage.setItem).not.toHaveBeenCalled();
    });

    it("should handle localStorage error", () => {
      mockLocalStorage.getItem.mockReturnValue("test-device-uuid");
      mockLocalStorage.setItem.mockImplementation(() => {
        throw new Error("Storage quota exceeded");
      });

      setDeviceTrustPreference(true);

      // Should not crash when error occurs
      expect(true).toBe(true);
    });

    it("should not execute in SSR environment", () => {
      const originalWindow = global.window;
      // @ts-ignore
      delete global.window;

      setDeviceTrustPreference(true);

      expect(mockLocalStorage.setItem).not.toHaveBeenCalled();

      // Restore window
      global.window = originalWindow;
    });

    it("should set false preference correctly", () => {
      mockLocalStorage.getItem.mockReturnValue("test-device-uuid");
      const mockNow = 1234567890000;
      jest.spyOn(Date, "now").mockReturnValue(mockNow);

      setDeviceTrustPreference(false);

      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        "device_trust_preference",
        JSON.stringify({
          trustDevice: false,
          deviceUuid: "test-device-uuid",
          timestamp: mockNow,
        }),
      );

      jest.restoreAllMocks();
    });
  });

  describe("clearDeviceTrustPreference", () => {
    it("should remove preference from localStorage", () => {
      clearDeviceTrustPreference();

      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith(
        "device_trust_preference",
      );
    });

    it("should handle localStorage error", () => {
      mockLocalStorage.removeItem.mockImplementation(() => {
        throw new Error("Cannot remove item");
      });

      clearDeviceTrustPreference();

      // Should not crash when error occurs
      expect(true).toBe(true);
    });

    it("should not execute in SSR environment", () => {
      const originalWindow = global.window;
      // @ts-ignore
      delete global.window;

      clearDeviceTrustPreference();

      expect(mockLocalStorage.removeItem).not.toHaveBeenCalled();

      // Restore window
      global.window = originalWindow;
    });
  });

  describe("shouldTrustDevice", () => {
    it("should return result from getDeviceTrustPreference", () => {
      const preference = {
        trustDevice: true,
        deviceUuid: "test-device-uuid",
        timestamp: Date.now(),
      };

      mockLocalStorage.getItem
        .mockReturnValueOnce(JSON.stringify(preference))
        .mockReturnValueOnce("test-device-uuid");

      const result = shouldTrustDevice();

      expect(result).toBe(true);
    });

    it("should return false when no preference", () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      const result = shouldTrustDevice();

      expect(result).toBe(false);
    });
  });
});
