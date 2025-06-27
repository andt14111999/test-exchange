import { renderHook, waitFor, act } from "@testing-library/react";
import { useDeviceTrust } from "@/hooks/use-device-trust";
import { getCurrentDevice } from "@/lib/api/device";
import { useUserStore } from "@/lib/store/user-store";
import type { CurrentDeviceResponse } from "@/lib/api/device";

// Mock the device API
jest.mock("@/lib/api/device", () => ({
  getCurrentDevice: jest.fn(),
}));

// Mock the user store
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

const mockGetCurrentDevice = getCurrentDevice as jest.MockedFunction<
  typeof getCurrentDevice
>;
const mockUseUserStore = useUserStore as jest.MockedFunction<
  typeof useUserStore
>;

const createMockDeviceResponse = (trusted: boolean): CurrentDeviceResponse => ({
  id: 1,
  device_type: "web",
  trusted,
  first_device: true,
  ip_address: "127.0.0.1",
  location: "Test Location",
  display_name: "Test Device",
  created_at: "2024-01-01T00:00:00Z",
});

describe("useDeviceTrust", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("returns initial state with device checking", () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(true));

    const { result } = renderHook(() => useDeviceTrust());

    expect(result.current.isCheckingDevice).toBe(true);
    expect(typeof result.current.setIsDeviceTrusted).toBe("function");
    expect(typeof result.current.recheckDeviceTrust).toBe("function");
  });

  it("sets device as trusted when user has no 2FA enabled", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: false },
    });

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
      expect(result.current.isDeviceTrusted).toBe(true);
    });

    expect(mockGetCurrentDevice).not.toHaveBeenCalled();
  });

  it("checks device trust status when 2FA is enabled", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(true));

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
      expect(result.current.isDeviceTrusted).toBe(true);
    });

    expect(mockGetCurrentDevice).toHaveBeenCalled();
  });

  it("sets device as not trusted when API returns false", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(false));

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
      expect(result.current.isDeviceTrusted).toBe(false);
    });
  });

  it("handles API error gracefully", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockRejectedValue(new Error("API Error"));

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
      expect(result.current.isDeviceTrusted).toBe(false);
    });
  });

  it("allows manual device trust state update", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(false));

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isDeviceTrusted).toBe(false);
    });

    // Manually set device as trusted
    act(() => {
      result.current.setIsDeviceTrusted(true);
    });

    await waitFor(() => {
      expect(result.current.isDeviceTrusted).toBe(true);
    });
  });

  it("recheckDeviceTrust function calls API again", async () => {
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(false));

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
    });

    // Clear previous calls
    mockGetCurrentDevice.mockClear();
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(true));

    // Call recheck
    await result.current.recheckDeviceTrust();

    await waitFor(() => {
      expect(result.current.isDeviceTrusted).toBe(true);
      expect(result.current.isCheckingDevice).toBe(false);
    });

    expect(mockGetCurrentDevice).toHaveBeenCalledTimes(1);
  });

  it("handles user with no 2FA info", async () => {
    mockUseUserStore.mockReturnValue({
      user: null,
    });

    const { result } = renderHook(() => useDeviceTrust());

    await waitFor(() => {
      expect(result.current.isCheckingDevice).toBe(false);
      expect(result.current.isDeviceTrusted).toBe(true);
    });
  });

  it("rechecks when user 2FA status changes", async () => {
    const { rerender } = renderHook(() => useDeviceTrust());

    // Initially no 2FA
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: false },
    });

    rerender();

    await waitFor(() => {
      expect(mockGetCurrentDevice).not.toHaveBeenCalled();
    });

    // Enable 2FA
    mockUseUserStore.mockReturnValue({
      user: { authenticatorEnabled: true },
    });
    mockGetCurrentDevice.mockResolvedValue(createMockDeviceResponse(true));

    rerender();

    await waitFor(() => {
      expect(mockGetCurrentDevice).toHaveBeenCalled();
    });
  });
});
