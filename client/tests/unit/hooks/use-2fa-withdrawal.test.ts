import { renderHook, act } from "@testing-library/react";
import { use2FAWithdrawal } from "@/hooks/use-2fa-withdrawal";
import { toast } from "sonner";
import { createWithdrawal } from "@/lib/api/withdrawals";
import { trustCurrentDevice } from "@/lib/api/device";

// Mock dependencies
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

jest.mock("@/lib/api/withdrawals", () => ({
  createWithdrawal: jest.fn(),
}));

jest.mock("@/lib/api/device", () => ({
  trustCurrentDevice: jest.fn(),
}));

jest.mock("@/lib/store/user-store", () => ({
  useUserStore: () => ({
    user: { authenticatorEnabled: true },
  }),
}));

jest.mock("@/hooks/use-device-trust", () => ({
  useDeviceTrust: () => ({
    isDeviceTrusted: false,
    isCheckingDevice: false,
    setIsDeviceTrusted: jest.fn(),
  }),
}));

const mockCreateWithdrawal = createWithdrawal as jest.MockedFunction<
  typeof createWithdrawal
>;
const mockTrustCurrentDevice = trustCurrentDevice as jest.MockedFunction<
  typeof trustCurrentDevice
>;
const mockToast = toast as jest.Mocked<typeof toast>;

describe("use2FAWithdrawal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should have correct initial state", () => {
    const { result } = renderHook(() => use2FAWithdrawal());

    expect(result.current.show2FADialog).toBe(false);
    expect(result.current.twoFactorError).toBe(null);
    expect(result.current.isSubmitting).toBe(false);
    expect(result.current.pendingWithdrawalData).toBe(null);
    expect(result.current.shouldShow2FAWarning).toBe(true);
  });

  it("should show 2FA dialog for untrusted device with 2FA enabled", async () => {
    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    expect(result.current.show2FADialog).toBe(true);
    expect(result.current.pendingWithdrawalData).toEqual(withdrawalData);
    expect(mockCreateWithdrawal).not.toHaveBeenCalled();
  });

  it("should handle 2FA submission successfully", async () => {
    mockCreateWithdrawal.mockResolvedValue({
      id: "1",
      coin_amount: 0.001,
      coin_fee: 0.0001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
      status: "PENDING",
      created_at: "2024-01-01T00:00:00Z",
      is_internal_transfer: false,
    });
    mockTrustCurrentDevice.mockResolvedValue({
      id: 1,
      device_type: "Browser",
      first_device: false,
      ip_address: "127.0.0.1",
      location: "Test Location",
      display_name: "Test Device",
      trusted: true,
      created_at: "2024-01-01T00:00:00Z",
    });

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    // First show 2FA dialog
    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    expect(result.current.show2FADialog).toBe(true);

    // Then submit 2FA
    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(mockCreateWithdrawal).toHaveBeenCalledWith({
      ...withdrawalData,
      two_factor_code: "123456",
    });
    expect(mockTrustCurrentDevice).toHaveBeenCalled();
    expect(result.current.show2FADialog).toBe(false);
    expect(result.current.pendingWithdrawalData).toBe(null);
    expect(mockToast.success).toHaveBeenCalledWith(
      "Withdrawal request submitted successfully",
    );
  });

  it("should handle 2FA submission error", async () => {
    const errorResponse = {
      response: { data: { error: "Invalid authentication code" } },
    };
    mockCreateWithdrawal.mockRejectedValue(errorResponse);

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    // Set up withdrawal
    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    // Submit 2FA with error
    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(result.current.twoFactorError).toBe("invalidCode");
    expect(result.current.show2FADialog).toBe(true); // Should stay open
  });

  it("should handle specific 2FA invalid code error", async () => {
    const errorResponse = {
      response: { data: { error: "Invalid code provided" } },
    };
    mockCreateWithdrawal.mockRejectedValue(errorResponse);

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(result.current.twoFactorError).toBe("invalidCode");
  });

  it("should close 2FA dialog correctly", async () => {
    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    expect(result.current.show2FADialog).toBe(true);

    act(() => {
      result.current.handleClose2FADialog();
    });

    expect(result.current.show2FADialog).toBe(false);
    expect(result.current.pendingWithdrawalData).toBe(null);
    expect(result.current.twoFactorError).toBe(null);
  });

  it("should not submit 2FA if no pending data", async () => {
    const { result } = renderHook(() => use2FAWithdrawal());

    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(mockCreateWithdrawal).not.toHaveBeenCalled();
  });

  it("should handle device trust check failure gracefully", async () => {
    mockCreateWithdrawal.mockResolvedValue({
      id: "1",
      coin_amount: 0.001,
      coin_fee: 0.0001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
      status: "PENDING",
      created_at: "2024-01-01T00:00:00Z",
      is_internal_transfer: false,
    });
    mockTrustCurrentDevice.mockRejectedValue(new Error("Device check failed"));

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    // Should still succeed even if device check fails
    expect(result.current.show2FADialog).toBe(false);
    expect(mockToast.success).toHaveBeenCalledWith(
      "Withdrawal request submitted successfully",
    );
  });

  it("should handle error response formats", async () => {
    // Test with string error
    mockCreateWithdrawal.mockRejectedValue("Network error");

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(result.current.twoFactorError).toBe("An unexpected error occurred");
  });

  it("should handle Error instance", async () => {
    mockCreateWithdrawal.mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => use2FAWithdrawal());

    const withdrawalData = {
      coin_amount: 0.001,
      coin_currency: "BTC",
      coin_layer: "mainnet",
      coin_address: "test-address",
    };

    await act(async () => {
      await result.current.handleWithdrawWithDevice(withdrawalData);
    });

    await act(async () => {
      await result.current.handle2FASubmit("123456");
    });

    expect(result.current.twoFactorError).toBe("Network error");
  });
});
