import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import TransactionHistoryPage from "@/app/[locale]/wallet/history/coin/[currency]/page";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { useCoinTransactions } from "@/hooks/use-coin-transactions";
import { getFiatDeposits } from "@/lib/api/fiat-deposits";
import { Transaction, TRANSACTION_STATUS } from "@/types/transaction";
import { FiatDeposit } from "@/types/fiat-deposits";
import { useToast } from "@/components/ui/use-toast";

// Mock the dependencies
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useParams: jest.fn(),
}));

jest.mock("@/hooks/use-coin-transactions", () => ({
  useCoinTransactions: jest.fn(),
}));

jest.mock("@/lib/api/fiat-deposits", () => ({
  getFiatDeposits: jest.fn(),
}));

jest.mock("@/components/ui/use-toast", () => ({
  useToast: jest.fn(),
}));

jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

jest.mock("@/components/transaction-history-tabs", () => ({
  TransactionHistoryTabs: ({
    transactions,
    isLoading,
    statusClassFn,
  }: {
    transactions: Transaction[];
    isLoading: boolean;
    statusClassFn: (status: string) => string;
  }) => (
    <div data-testid="transaction-tabs">
      {isLoading ? (
        <div>Loading...</div>
      ) : (
        <div>
          {transactions.map((tx) => (
            <div key={tx.id} className={statusClassFn(tx.status)}>
              {tx.type} - {tx.amount} - {tx.status}
            </div>
          ))}
        </div>
      )}
    </div>
  ),
}));

describe("TransactionHistoryPage", () => {
  const mockToast = jest.fn();
  const mockTranslations = {
    "wallet.history.title": "Transaction History",
    "deposit.refreshHistory": "Refresh History",
    "deposit.errorFetchingHistory": "Failed to load deposit history",
    "deposit.somethingWentWrong": "Something went wrong",
    "common.errors.failedToLoad": "Failed to load",
  };

  const mockCoinTransactions = {
    data: {
      deposits: [
        {
          id: "1",
          amount: 100,
          status: TRANSACTION_STATUS.COMPLETED,
          created_at: "2024-03-20T10:00:00Z",
          updated_at: "2024-03-20T10:00:00Z",
          coin_currency: "btc",
          hash: "0x123",
        },
      ],
      withdrawals: [
        {
          id: "2",
          amount: 50,
          status: TRANSACTION_STATUS.PENDING,
          created_at: "2024-03-19T10:00:00Z",
          updated_at: "2024-03-19T10:00:00Z",
          coin_currency: "btc",
          hash: "0x456",
        },
      ],
    },
  };

  const mockFiatDeposits: FiatDeposit[] = [
    {
      id: "3",
      fiat_amount: 1000000,
      currency: "vnd",
      status: "processed",
      created_at: "2024-03-18T10:00:00Z",
      country_code: "VN",
      deposit_fee: 0,
      amount_after_fee: 1000000,
      user_id: "user1",
      fiat_account_id: "account1",
      requires_ownership_verification: false,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key as keyof typeof mockTranslations],
    );
    (useParams as jest.Mock).mockReturnValue({ currency: "btc" });
    (useCoinTransactions as jest.Mock).mockReturnValue({
      data: mockCoinTransactions,
      isLoading: false,
      error: null,
    });
    (getFiatDeposits as jest.Mock).mockResolvedValue([]);
    (useToast as jest.Mock).mockReturnValue({ toast: mockToast });
  });

  it("renders crypto currency transactions correctly", async () => {
    render(<TransactionHistoryPage />);

    expect(screen.getByText("Transaction History - BTC")).toBeInTheDocument();
    expect(screen.getByTestId("transaction-tabs")).toBeInTheDocument();
    expect(screen.getByText("deposit - 100 - completed")).toBeInTheDocument();
    expect(screen.getByText("withdrawal - 50 - pending")).toBeInTheDocument();
  });

  it("renders loading state correctly", () => {
    (useCoinTransactions as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(<TransactionHistoryPage />);

    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("renders error state correctly", () => {
    (useCoinTransactions as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error("Failed to load"),
    });

    render(<TransactionHistoryPage />);

    expect(screen.getByText("Failed to load")).toBeInTheDocument();
  });

  it("fetches and renders fiat deposits for VND currency", async () => {
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    (getFiatDeposits as jest.Mock).mockResolvedValue(mockFiatDeposits);

    render(<TransactionHistoryPage />);

    expect(screen.getByText("Transaction History - VND")).toBeInTheDocument();
    expect(screen.getByText("Refresh History")).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByText("fiat_deposit - 1000000 - processed"),
      ).toBeInTheDocument();
    });
  });

  it("handles fiat deposit fetch error correctly", async () => {
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    const mockError = new Error("API Error");
    (getFiatDeposits as jest.Mock).mockRejectedValue(mockError);

    render(<TransactionHistoryPage />);

    const refreshButton = screen.getByText("Refresh History");
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: "Failed to load deposit history",
        description: "Something went wrong",
        variant: "destructive",
      });
    });
  });

  it("refreshes fiat deposits when button is clicked", async () => {
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    (getFiatDeposits as jest.Mock).mockResolvedValue(mockFiatDeposits);

    render(<TransactionHistoryPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(getFiatDeposits).toHaveBeenCalledTimes(1);
    });

    const refreshButton = screen.getByText("Refresh History");
    fireEvent.click(refreshButton);

    // Wait for the second call
    await waitFor(() => {
      expect(getFiatDeposits).toHaveBeenCalledTimes(2);
    });
  });

  it("disables refresh button while loading", async () => {
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    let resolvePromise!: (value: FiatDeposit[]) => void;
    const promise = new Promise<FiatDeposit[]>((resolve) => {
      resolvePromise = resolve;
    });
    (getFiatDeposits as jest.Mock).mockReturnValue(promise);

    render(<TransactionHistoryPage />);

    const refreshButton = screen.getByText("Refresh History");
    fireEvent.click(refreshButton);

    expect(refreshButton).toBeDisabled();

    resolvePromise(mockFiatDeposits);
    await waitFor(() => {
      expect(refreshButton).not.toBeDisabled();
    });
  });

  it("sorts transactions by date correctly", async () => {
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    (getFiatDeposits as jest.Mock).mockResolvedValue(mockFiatDeposits);

    render(<TransactionHistoryPage />);

    await waitFor(() => {
      const transactions = screen.getAllByText(
        /deposit|withdrawal|fiat_deposit/,
      );
      expect(transactions[0].textContent).toContain("deposit - 100");
      expect(transactions[1].textContent).toContain("withdrawal - 50");
      expect(transactions[2].textContent).toContain("fiat_deposit - 1000000");
    });
  });
});
