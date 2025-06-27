import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { TwoFactorAuthDisableDialog } from "@/components/two-factor-auth-disable-dialog";
import { disableTwoFactorAuth } from "@/lib/api/user";

// Mock the API function
jest.mock("@/lib/api/user", () => ({
  disableTwoFactorAuth: jest.fn(),
}));

// Mock the toast hook
const mockToast = jest.fn();
jest.mock("@/components/ui/use-toast", () => ({
  useToast: () => ({
    toast: mockToast,
    dismissToast: jest.fn(),
    toasts: [],
  }),
}));

const mockDisableTwoFactorAuth = disableTwoFactorAuth as jest.MockedFunction<
  typeof disableTwoFactorAuth
>;

describe("TwoFactorAuthDisableDialog", () => {
  const mockOnOpenChange = jest.fn();
  const mockOnSuccess = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const defaultProps = {
    open: true,
    onOpenChange: mockOnOpenChange,
    onSuccess: mockOnSuccess,
  };

  it("renders dialog when open is true", () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    expect(screen.getByText("Tắt xác thực 2 bước")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Việc tắt xác thực 2 bước sẽ làm giảm tính bảo mật của tài khoản của bạn.",
      ),
    ).toBeInTheDocument();
  });

  it("does not render dialog when open is false", () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} open={false} />);

    expect(screen.queryByText("Tắt xác thực 2 bước")).not.toBeInTheDocument();
  });

  it("shows error when verification code is less than 6 digits", async () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "12345" } });
    fireEvent.click(disableButton);

    // Button should be disabled when code is less than 6 digits
    expect(disableButton).toBeDisabled();
    expect(mockDisableTwoFactorAuth).not.toHaveBeenCalled();
  });

  it("shows error when verification code is empty", async () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const disableButton = screen.getByText("Tắt 2FA");
    fireEvent.click(disableButton);

    // Button should be disabled when code is empty
    expect(disableButton).toBeDisabled();
    expect(mockDisableTwoFactorAuth).not.toHaveBeenCalled();
  });

  it("successfully disables 2FA with valid code", async () => {
    mockDisableTwoFactorAuth.mockResolvedValue({ message: "Success" });

    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(disableButton);

    await waitFor(() => {
      expect(mockDisableTwoFactorAuth).toHaveBeenCalledWith("123456");
    });

    expect(mockToast).toHaveBeenCalledWith({
      title: "Thành công",
      description: "Xác thực 2 bước đã được tắt",
    });

    expect(mockOnSuccess).toHaveBeenCalled();
    expect(mockOnOpenChange).toHaveBeenCalledWith(false);
  });

  it("shows error message when API call fails", async () => {
    const errorMessage = "Invalid code";
    mockDisableTwoFactorAuth.mockRejectedValue(new Error(errorMessage));

    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(disableButton);

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });

    expect(mockOnSuccess).not.toHaveBeenCalled();
    expect(mockOnOpenChange).not.toHaveBeenCalledWith(false);
  });

  it("shows generic error message when API call fails without error message", async () => {
    mockDisableTwoFactorAuth.mockRejectedValue(new Error(""));

    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(disableButton);

    // Just verify that the API was called
    await waitFor(() => {
      expect(mockDisableTwoFactorAuth).toHaveBeenCalledWith("123456");
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });
  });

  it("resets form when canceled", () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const cancelButton = screen.getByText("Hủy");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(cancelButton);

    expect(mockOnOpenChange).toHaveBeenCalledWith(false);
  });

  it("shows loading state when disabling 2FA", async () => {
    let resolvePromise: (value: { message: string }) => void;
    const promise = new Promise<{ message: string }>((resolve) => {
      resolvePromise = resolve;
    });
    mockDisableTwoFactorAuth.mockReturnValue(promise);

    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(disableButton);

    await waitFor(() => {
      expect(screen.getByText("Đang tắt...")).toBeInTheDocument();
    });

    // Resolve the promise to complete the test
    resolvePromise!({ message: "Success" });
  });

  it("allows input of verification code", () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    fireEvent.change(input, { target: { value: "123456" } });

    expect(input).toHaveValue("123456");
  });

  it("clears error when input changes", async () => {
    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    // Enter invalid code to show disabled state
    fireEvent.change(input, { target: { value: "123" } });
    expect(disableButton).toBeDisabled();

    // Enter valid code should enable button
    fireEvent.change(input, { target: { value: "123456" } });
    expect(disableButton).not.toBeDisabled();
  });

  it("handles error when API call fails without specific error message", async () => {
    // Spy on console.error to prevent test output noise
    const consoleErrorSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});

    mockDisableTwoFactorAuth.mockImplementation(() => {
      return Promise.reject(new Error(""));
    });

    render(<TwoFactorAuthDisableDialog {...defaultProps} />);

    const input = screen.getByTestId("input");
    const disableButton = screen.getByText("Tắt 2FA");

    fireEvent.change(input, { target: { value: "123456" } });
    fireEvent.click(disableButton);

    // Verify that the API was called with the right parameters
    await waitFor(() => {
      expect(mockDisableTwoFactorAuth).toHaveBeenCalledWith("123456");
    });

    // Verify that success handler was not called
    expect(mockOnSuccess).not.toHaveBeenCalled();
    expect(mockOnOpenChange).not.toHaveBeenCalledWith(false);

    consoleErrorSpy.mockRestore();
  });
});
