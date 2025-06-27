import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { WalletList } from "@/components/wallet-list";
import { useRouter } from "@/navigation";
import { CoinAccount, FiatAccount } from "@/types";

// Mock the next-intl hooks
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    switch (key) {
      case "wallet.deposit":
        return "Deposit";
      case "wallet.withdraw":
        return "Withdraw";
      case "wallet.swap":
        return "Swap";
      case "wallet.viewHistory":
        return "View History";
      case "wallet.crypto":
        return "Crypto";
      case "wallet.fiat":
        return "Fiat";
      case "wallet.token":
        return "Token";
      case "wallet.balance":
        return "Balance";
      case "wallet.frozen":
        return "Frozen";
      case "wallet.actions":
        return "Actions";
      default:
        return key;
    }
  },
}));

// Mock the navigation hooks
jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock the currencies config
jest.mock("@/config/currencies", () => ({
  CURRENCIES: {
    BTC: {
      name: "Bitcoin",
      symbol: "BTC",
      type: "crypto",
      icon: "₿",
    },
    USD: {
      name: "US Dollar",
      symbol: "USD",
      type: "fiat",
      icon: "$",
    },
  },
}));

// Add mock coinSettings for BTC (đặt ngoài describe để các test dùng lại)
const mockCoinSettings = [
  {
    id: 1,
    currency: "BTC",
    deposit_enabled: true,
    withdraw_enabled: true,
    swap_enabled: true,
    layers: [],
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
];

describe("WalletList", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  const mockCoinAccount: CoinAccount = {
    id: 1,
    user_id: 1,
    coin_currency: "BTC",
    balance: 1.23456789,
    frozen_balance: 0.1,
    created_at: "2024-03-20T10:00:00Z",
    updated_at: "2024-03-20T10:00:00Z",
  };

  const mockFiatAccount: FiatAccount = {
    id: 2,
    user_id: 1,
    currency: "USD",
    balance: 1000.5,
    frozen_balance: 100,
    created_at: "2024-03-20T10:00:00Z",
    updated_at: "2024-03-20T10:00:00Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
  });

  it("renders crypto accounts correctly", () => {
    render(<WalletList accounts={[mockCoinAccount]} type="crypto" />);

    // Check if the account details are rendered correctly
    expect(screen.getByText("Bitcoin")).toBeInTheDocument();
    expect(
      screen.getByText("BTC", { selector: ".inline-flex" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Crypto")).toBeInTheDocument();
    expect(screen.getByText("1.23456789")).toBeInTheDocument();
    expect(screen.getByText("0.10")).toBeInTheDocument();
  });

  it("renders fiat accounts correctly", () => {
    render(<WalletList accounts={[mockFiatAccount]} type="fiat" />);

    // Check if the account details are rendered correctly
    expect(screen.getByText("US Dollar")).toBeInTheDocument();
    expect(
      screen.getByText("USD", { selector: ".inline-flex" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Fiat")).toBeInTheDocument();
    expect(screen.getByText("1,000.50")).toBeInTheDocument();
    expect(screen.getByText("100.00")).toBeInTheDocument();
  });

  it("handles deposit button click correctly", async () => {
    const user = userEvent.setup();
    render(
      <WalletList
        accounts={[mockCoinAccount]}
        type="crypto"
        coinSettings={mockCoinSettings}
      />,
    );

    const depositButton = screen.getByRole("button", {
      name: "Deposit",
    });
    expect(depositButton).not.toBeDisabled();
    await user.click(depositButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/deposit/btc");
  });

  it("handles withdraw button click correctly", async () => {
    const user = userEvent.setup();
    render(
      <WalletList
        accounts={[mockCoinAccount]}
        type="crypto"
        coinSettings={mockCoinSettings}
      />,
    );

    const withdrawButton = screen.getByRole("button", {
      name: "Withdraw",
    });
    expect(withdrawButton).not.toBeDisabled();
    await user.click(withdrawButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/withdraw/btc");
  });

  it("handles swap button click correctly", async () => {
    const user = userEvent.setup();
    render(
      <WalletList
        accounts={[mockCoinAccount]}
        type="crypto"
        coinSettings={mockCoinSettings}
      />,
    );

    const swapButton = screen.getByRole("button", { name: "Swap" });
    expect(swapButton).not.toBeDisabled();
    await user.click(swapButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/swap?token=BTC");
  });

  it("handles view history button click correctly for crypto", async () => {
    const user = userEvent.setup();
    render(<WalletList accounts={[mockCoinAccount]} type="crypto" />);

    const historyButton = screen.getByRole("button", {
      name: "View History",
    });
    await user.click(historyButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/wallet/history/coin/btc");
  });

  it("handles view history button click correctly for fiat", async () => {
    const user = userEvent.setup();
    render(<WalletList accounts={[mockFiatAccount]} type="fiat" />);

    const historyButton = screen.getByRole("button", {
      name: "View History",
    });
    await user.click(historyButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/wallet/history/fiat/usd");
  });

  it("renders empty state correctly", () => {
    render(<WalletList accounts={[]} type="crypto" />);

    // Check if the table is rendered with headers but no data
    expect(screen.getByText("Token")).toBeInTheDocument();
    expect(screen.getByText("Balance")).toBeInTheDocument();
    expect(screen.getByText("Frozen")).toBeInTheDocument();
    expect(screen.getByText("Actions")).toBeInTheDocument();
  });

  it("handles unknown currency correctly", () => {
    const unknownCoinAccount: CoinAccount = {
      ...mockCoinAccount,
      coin_currency: "UNKNOWN",
    };

    render(<WalletList accounts={[unknownCoinAccount]} type="crypto" />);

    // Should use the currency code as fallback
    expect(
      screen.getByText("UNKNOWN", { selector: ".inline-flex" }),
    ).toBeInTheDocument();
  });

  it("formats numbers correctly", () => {
    const accountWithLongDecimals: CoinAccount = {
      ...mockCoinAccount,
      balance: 1.234567891234,
      frozen_balance: 0.000000001,
    };

    render(<WalletList accounts={[accountWithLongDecimals]} type="crypto" />);

    // Should format with maximum 8 decimal places
    const balanceSpan = screen.getByText("1.23456789", {
      selector: ".font-medium",
    });
    const frozenSpan = screen.getByText("0.00", { selector: ".font-medium" });

    expect(balanceSpan).toBeInTheDocument();
    expect(frozenSpan).toBeInTheDocument();
  });

  it("handles zero balances correctly", () => {
    const accountWithZeroBalances: CoinAccount = {
      ...mockCoinAccount,
      balance: 0,
      frozen_balance: 0,
    };

    const { container } = render(
      <WalletList accounts={[accountWithZeroBalances]} type="crypto" />,
    );

    // Get the balance cell (second column) and check its value
    const balanceCell = container.querySelector("tr td:nth-child(2)");
    const balanceSpan = balanceCell?.querySelector(".font-medium");
    expect(balanceSpan).toHaveTextContent("0.00");

    // Get the frozen balance cell (third column) and check its value
    const frozenCell = container.querySelector("tr td:nth-child(3)");
    const frozenSpan = frozenCell?.querySelector(".font-medium");
    expect(frozenSpan).toHaveTextContent("0.00");
  });
});
