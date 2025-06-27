// Mock next-intl inline
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => `translated.${key}`,
}));

import { render, screen, fireEvent } from "@testing-library/react";
import { ExternalWithdrawTab } from "@/app/[locale]/withdraw/usdt/components/external-withdraw-tab";
import { Network } from "@/app/[locale]/withdraw/usdt/components/types";

const mockNetworks: Network[] = [
  {
    id: "bep20",
    name: "BNB Smart Chain (BEP20)",
    enabled: true,
    fee: 1,
  },
  {
    id: "trc20",
    name: "TRON (TRC20)",
    enabled: true,
    fee: 2,
  },
  {
    id: "erc20",
    name: "Ethereum (ERC20)",
    enabled: false,
    fee: 5,
  },
];

const defaultProps = {
  networks: mockNetworks,
  selectedNetwork: null,
  onNetworkChange: jest.fn(),
  address: "",
  onAddressChange: jest.fn(),
  addressError: null,
  amount: "",
  onAmountChange: jest.fn(),
  amountError: null,
  usdtBalance: 1000,
  isLoadingWallet: false,
  isLoadingNetworks: false,
  userHas2FA: false,
  isDeviceTrusted: true,
  isCheckingDevice: false,
};

describe("ExternalWithdrawTab", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders all form fields correctly", () => {
    render(<ExternalWithdrawTab {...defaultProps} />);

    expect(screen.getByText("translated.network")).toBeInTheDocument();
    expect(
      screen.getByText("translated.destinationAddress"),
    ).toBeInTheDocument();
    expect(screen.getByText("translated.amount")).toBeInTheDocument();
    expect(screen.getByText("translated.selectNetwork")).toBeInTheDocument();
  });

  it("displays network options correctly", () => {
    render(<ExternalWithdrawTab {...defaultProps} />);

    // Click select trigger to open dropdown
    fireEvent.click(screen.getByRole("combobox"));

    // Check enabled networks
    expect(screen.getByText("BNB Smart Chain (BEP20)")).toBeInTheDocument();
    expect(screen.getByText("TRON (TRC20)")).toBeInTheDocument();

    // Check disabled network with indicator
    expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
  });

  it("calls onNetworkChange when network is selected", () => {
    const onNetworkChange = jest.fn();
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        onNetworkChange={onNetworkChange}
      />,
    );

    // Click select trigger to open dropdown
    fireEvent.click(screen.getByRole("combobox"));

    // Click on BEP20 option
    fireEvent.click(screen.getByText("BNB Smart Chain (BEP20)"));

    expect(onNetworkChange).toHaveBeenCalledWith(mockNetworks[0]);
  });

  it("calls onAddressChange when address input changes", () => {
    const onAddressChange = jest.fn();
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        selectedNetwork={mockNetworks[0]}
        onAddressChange={onAddressChange}
      />,
    );

    const addressInput = screen.getByPlaceholderText("translated.enterAddress");
    fireEvent.change(addressInput, { target: { value: "0x1234567890" } });

    expect(onAddressChange).toHaveBeenCalledWith("0x1234567890");
  });

  it("calls onAmountChange when amount input changes", () => {
    const onAmountChange = jest.fn();
    render(
      <ExternalWithdrawTab {...defaultProps} onAmountChange={onAmountChange} />,
    );

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    fireEvent.change(amountInput, { target: { value: "100" } });

    expect(onAmountChange).toHaveBeenCalledWith("100");
  });

  it("displays address error when provided", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        addressError="Invalid address format"
      />,
    );

    expect(screen.getByText("Invalid address format")).toBeInTheDocument();
  });

  it("displays amount error when provided", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        amountError="Insufficient balance"
      />,
    );

    expect(screen.getByText("Insufficient balance")).toBeInTheDocument();
  });

  it("shows correct network fee", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        selectedNetwork={mockNetworks[0]}
      />,
    );

    const networkFeeElements = screen.getAllByText("translated.networkFee");
    expect(networkFeeElements.length).toBeGreaterThan(0);
  });

  it("shows total amount when amount is entered", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        selectedNetwork={mockNetworks[0]}
        amount="100"
      />,
    );

    expect(screen.getByText("translated.totalAmount")).toBeInTheDocument();
  });

  it("shows 2FA required message when applicable", () => {
    render(
      <ExternalWithdrawTab
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

  it("shows address examples based on selected network", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        selectedNetwork={mockNetworks[0]} // BEP20
      />,
    );

    expect(
      screen.getByText(/0x71C7656EC7ab88b098defB751B7401B5f6d8976F/),
    ).toBeInTheDocument();
  });

  it("applies error styling to inputs when errors exist", () => {
    render(
      <ExternalWithdrawTab
        {...defaultProps}
        selectedNetwork={mockNetworks[0]}
        addressError="Invalid address"
        amountError="Invalid amount"
      />,
    );

    const addressInput = screen.getByPlaceholderText("translated.enterAddress");
    const amountInput = screen.getByPlaceholderText("translated.enterAmount");

    expect(addressInput).toHaveClass("border-red-500");
    expect(amountInput).toHaveClass("border-red-500");
  });
});
