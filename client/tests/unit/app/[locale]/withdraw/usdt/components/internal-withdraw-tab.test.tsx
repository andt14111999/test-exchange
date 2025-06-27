// Mock useTranslations
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => `translated.${key}`,
}));

import { render, screen, fireEvent } from "@testing-library/react";
import { InternalWithdrawTab } from "@/app/[locale]/withdraw/usdt/components/internal-withdraw-tab";

const defaultProps = {
  username: "",
  onUsernameChange: jest.fn(),
  usernameError: null,
  isValidatingUsername: false,
  amount: "",
  onAmountChange: jest.fn(),
  amountError: null,
  usdtBalance: 1000,
  isLoadingWallet: false,
  userHas2FA: false,
  isDeviceTrusted: true,
  isCheckingDevice: false,
};

describe("InternalWithdrawTab", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders all form fields correctly", () => {
    render(<InternalWithdrawTab {...defaultProps} />);

    expect(
      screen.getByText("translated.recipientUsername"),
    ).toBeInTheDocument();
    expect(screen.getByText("translated.amount")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("translated.enterUsername"),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("translated.enterAmount"),
    ).toBeInTheDocument();
  });

  it("calls onUsernameChange when username input changes", () => {
    const onUsernameChange = jest.fn();
    render(
      <InternalWithdrawTab
        {...defaultProps}
        onUsernameChange={onUsernameChange}
      />,
    );

    const usernameInput = screen.getByPlaceholderText(
      "translated.enterUsername",
    );
    fireEvent.change(usernameInput, { target: { value: "testuser" } });

    expect(onUsernameChange).toHaveBeenCalledWith("testuser");
  });

  it("calls onAmountChange when amount input changes", () => {
    const onAmountChange = jest.fn();
    render(
      <InternalWithdrawTab {...defaultProps} onAmountChange={onAmountChange} />,
    );

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    fireEvent.change(amountInput, { target: { value: "100" } });

    expect(onAmountChange).toHaveBeenCalledWith("100");
  });

  it("displays username error when provided", () => {
    render(
      <InternalWithdrawTab
        {...defaultProps}
        usernameError="Invalid username"
      />,
    );

    expect(screen.getByText("Invalid username")).toBeInTheDocument();
  });

  it("displays amount error when provided", () => {
    render(
      <InternalWithdrawTab
        {...defaultProps}
        amountError="Insufficient balance"
      />,
    );

    expect(screen.getByText("Insufficient balance")).toBeInTheDocument();
  });

  it("shows validating username message when isValidatingUsername is true", () => {
    render(
      <InternalWithdrawTab {...defaultProps} isValidatingUsername={true} />,
    );

    expect(
      screen.getByText("translated.validatingUsername"),
    ).toBeInTheDocument();
  });

  it("shows username example text", () => {
    render(<InternalWithdrawTab {...defaultProps} />);

    expect(screen.getByText("translated.usernameExample")).toBeInTheDocument();
  });

  it("shows total amount when amount is entered", () => {
    render(<InternalWithdrawTab {...defaultProps} amount="100" />);

    expect(screen.getByText("translated.totalAmount")).toBeInTheDocument();
  });

  it("shows 2FA required message when applicable", () => {
    render(
      <InternalWithdrawTab
        {...defaultProps}
        userHas2FA={true}
        isDeviceTrusted={false}
        isCheckingDevice={false}
      />,
    );

    expect(
      screen.getByText("translated.twoFactorRequired"),
    ).toBeInTheDocument();
  });

  it("displays important information list", () => {
    render(<InternalWithdrawTab {...defaultProps} />);

    expect(screen.getByText("translated.minTransfer")).toBeInTheDocument();
    expect(screen.getByText("translated.noNetworkFees")).toBeInTheDocument();
    expect(screen.getByText("translated.instantTransfers")).toBeInTheDocument();
  });

  it("applies error styling to inputs when errors exist", () => {
    render(
      <InternalWithdrawTab
        {...defaultProps}
        usernameError="Invalid username"
        amountError="Invalid amount"
      />,
    );

    const usernameInput = screen.getByPlaceholderText(
      "translated.enterUsername",
    );
    const amountInput = screen.getByPlaceholderText("translated.enterAmount");

    expect(usernameInput).toHaveClass("border-red-500");
    expect(amountInput).toHaveClass("border-red-500");
  });

  it("shows loading state for wallet balance", () => {
    render(<InternalWithdrawTab {...defaultProps} isLoadingWallet={true} />);

    expect(screen.getByText("translated.availableBalance")).toBeInTheDocument();
  });
});
