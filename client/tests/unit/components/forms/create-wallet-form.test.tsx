import { render, screen, fireEvent } from "@testing-library/react";
import { CreateWalletForm } from "@/components/forms/create-wallet-form";
import { useRouter } from "@/navigation";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

// Mock navigation
jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

describe("CreateWalletForm", () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
    });
  });

  it("renders the form with all elements", () => {
    render(<CreateWalletForm />);

    // Check if title and description are rendered
    expect(screen.getByText("wallet.createWallet.title")).toBeInTheDocument();
    expect(
      screen.getByText("wallet.createWallet.description"),
    ).toBeInTheDocument();

    // Check if currency selector is rendered
    expect(
      screen.getByText("wallet.createWallet.selectCurrency"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("wallet.createWallet.selectCurrencyPlaceholder"),
    ).toBeInTheDocument();

    // Check if create button is rendered and initially disabled
    const createButton = screen.getByRole("button", {
      name: "wallet.createWallet.createButton",
    });
    expect(createButton).toBeInTheDocument();
    expect(createButton).toBeDisabled();
  });

  it("enables create button when currency is selected", async () => {
    render(<CreateWalletForm />);

    // Get the currency select trigger
    const currencySelect = screen.getByRole("combobox");
    expect(currencySelect).toBeInTheDocument();

    // Open the select dropdown
    fireEvent.click(currencySelect);

    // Select Bitcoin
    const bitcoinOption = screen.getByText("Bitcoin (BTC)");
    fireEvent.click(bitcoinOption);

    // Check if create button is enabled
    const createButton = screen.getByRole("button", {
      name: "wallet.createWallet.createButton",
    });
    expect(createButton).toBeEnabled();
  });

  it("navigates to wallet page when create button is clicked", () => {
    render(<CreateWalletForm />);

    // Get the currency select trigger
    const currencySelect = screen.getByRole("combobox");
    fireEvent.click(currencySelect);

    // Select Bitcoin
    const bitcoinOption = screen.getByText("Bitcoin (BTC)");
    fireEvent.click(bitcoinOption);

    // Click create button
    const createButton = screen.getByRole("button", {
      name: "wallet.createWallet.createButton",
    });
    fireEvent.click(createButton);

    // Check if router.push was called with correct path
    expect(mockPush).toHaveBeenCalledWith("/wallet");
  });

  it("allows selecting different currencies", () => {
    render(<CreateWalletForm />);

    // Get the currency select trigger
    const currencySelect = screen.getByRole("combobox");

    // Test each currency option
    const currencies = [
      { label: "Bitcoin (BTC)", value: "btc" },
      { label: "Ethereum (ETH)", value: "eth" },
      { label: "Tether (USDT)", value: "usdt" },
      { label: "USD Coin (USDC)", value: "usdc" },
    ];

    currencies.forEach((currency) => {
      // Open select dropdown
      fireEvent.click(currencySelect);

      // Select currency
      const option = screen.getByText(currency.label);
      fireEvent.click(option);

      // Verify selection
      expect(currencySelect).toHaveTextContent(currency.label);
    });
  });
});
