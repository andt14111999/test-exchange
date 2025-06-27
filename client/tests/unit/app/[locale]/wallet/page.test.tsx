import { render, screen, waitFor } from "@testing-library/react";
import WalletPage from "@/app/[locale]/wallet/page";
import { useWallet } from "@/hooks/use-wallet";
import { CoinAccount, FiatAccount } from "@/types";

// Mock the hooks
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => {
    if (key === "wallet.coinAccounts.title") return "Crypto Accounts";
    if (key === "wallet.fiatAccounts.title") return "Fiat Accounts";
    if (key === "common.errors.failedToLoad") return "Failed to load";
    if (key === "wallet.title") return "Wallet";
    return key;
  }),
}));

jest.mock("@/hooks/use-wallet", () => ({
  useWallet: jest.fn(),
}));

// Mock fetchCoinSettings
jest.mock("@/lib/api/coins", () => ({
  fetchCoinSettings: jest.fn(() =>
    Promise.resolve([
      { id: 1, coin_currency: "BTC", name: "Bitcoin", is_active: true },
      { id: 2, coin_currency: "ETH", name: "Ethereum", is_active: true },
    ]),
  ),
}));

// Mock the components
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({
    children,
  }: {
    children: React.ReactNode;
    loadingFallback: React.ReactNode;
  }) => <div data-testid="protected-layout">{children}</div>,
}));

jest.mock("@/components/wallet-list", () => ({
  WalletList: ({
    accounts,
    type,
  }: {
    accounts: (CoinAccount | FiatAccount)[];
    type: "crypto" | "fiat";
  }) => (
    <div data-testid={`wallet-list-${type}`}>
      {accounts.map((account, index) => (
        <div key={index} data-testid={`account-${type}-${index}`}>
          {type === "crypto"
            ? (account as CoinAccount).coin_currency
            : (account as FiatAccount).currency}
        </div>
      ))}
    </div>
  ),
}));

jest.mock("@/components/forms/create-wallet-form", () => ({
  CreateWalletForm: () => (
    <div data-testid="create-wallet-form">Create Wallet Form</div>
  ),
}));

describe("WalletPage", () => {
  const mockWalletData = {
    coin_accounts: [
      {
        id: 1,
        user_id: 1,
        coin_currency: "BTC",
        balance: 1.5,
        frozen_balance: 0.5,
        created_at: "2024-03-20T00:00:00Z",
        updated_at: "2024-03-20T00:00:00Z",
      },
      {
        id: 2,
        user_id: 1,
        coin_currency: "ETH",
        balance: 10,
        frozen_balance: 1,
        created_at: "2024-03-20T00:00:00Z",
        updated_at: "2024-03-20T00:00:00Z",
      },
    ],
    fiat_accounts: [
      {
        id: 3,
        user_id: 1,
        currency: "USD",
        balance: 1000,
        frozen_balance: 100,
        created_at: "2024-03-20T00:00:00Z",
        updated_at: "2024-03-20T00:00:00Z",
      },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading state correctly", () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(<WalletPage />);

    // Should show skeleton elements in the content area
    const skeletonElements = document.getElementsByClassName("animate-pulse");
    expect(skeletonElements.length).toBeGreaterThan(0);
  });

  it("renders error state correctly", async () => {
    const errorMessage = "Failed to load wallet data";
    (useWallet as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error(errorMessage),
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
      expect(screen.getByText(errorMessage)).toHaveClass("text-destructive");
    });
  });

  it("renders generic error message when error is not an instance of Error", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: "Some error",
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.getByText("Failed to load")).toBeInTheDocument();
    });
  });

  it("renders create wallet form when no accounts exist", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: { coin_accounts: [], fiat_accounts: [] },
      isLoading: false,
      error: null,
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.getByTestId("create-wallet-form")).toBeInTheDocument();
    });
  });

  it("renders create wallet form when wallet data is null", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.getByTestId("create-wallet-form")).toBeInTheDocument();
    });
  });

  it("renders both crypto and fiat accounts when they exist", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: mockWalletData,
      isLoading: false,
      error: null,
    });

    render(<WalletPage />);

    await waitFor(() => {
      // Check section titles
      expect(screen.getByText("Crypto Accounts")).toBeInTheDocument();
      expect(screen.getByText("Fiat Accounts")).toBeInTheDocument();

      // Check wallet lists
      expect(screen.getByTestId("wallet-list-crypto")).toBeInTheDocument();
      expect(screen.getByTestId("wallet-list-fiat")).toBeInTheDocument();

      // Check individual accounts
      expect(screen.getByTestId("account-crypto-0")).toHaveTextContent("BTC");
      expect(screen.getByTestId("account-crypto-1")).toHaveTextContent("ETH");
      expect(screen.getByTestId("account-fiat-0")).toHaveTextContent("USD");
    });
  });

  it("renders only crypto accounts when no fiat accounts exist", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: mockWalletData.coin_accounts,
        fiat_accounts: [],
      },
      isLoading: false,
      error: null,
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.getByText("Crypto Accounts")).toBeInTheDocument();
      expect(screen.queryByText("Fiat Accounts")).not.toBeInTheDocument();
      expect(screen.getByTestId("wallet-list-crypto")).toBeInTheDocument();
      expect(screen.queryByTestId("wallet-list-fiat")).not.toBeInTheDocument();
    });
  });

  it("renders only fiat accounts when no crypto accounts exist", async () => {
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: [],
        fiat_accounts: mockWalletData.fiat_accounts,
      },
      isLoading: false,
      error: null,
    });

    render(<WalletPage />);

    await waitFor(() => {
      expect(screen.queryByText("Crypto Accounts")).not.toBeInTheDocument();
      expect(screen.getByText("Fiat Accounts")).toBeInTheDocument();
      expect(
        screen.queryByTestId("wallet-list-crypto"),
      ).not.toBeInTheDocument();
      expect(screen.getByTestId("wallet-list-fiat")).toBeInTheDocument();
    });
  });
});
