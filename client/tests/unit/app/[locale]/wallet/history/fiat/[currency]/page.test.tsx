import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import FiatTransactionHistoryPage from "@/app/[locale]/wallet/history/fiat/[currency]/page";
import { useParams, useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { getFiatDeposits, getFiatWithdrawals } from "@/lib/api/fiat-deposits";
import { useToast } from "@/components/ui/use-toast";
import { AxiosError, AxiosResponse } from "axios";
import { Transaction } from "@/types/transaction";
import { FiatTransaction } from "@/types/fiat-transaction";

// Mock the dependencies
jest.mock("next/navigation", () => ({
  useParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

jest.mock("@/lib/api/fiat-deposits", () => ({
  getFiatDeposits: jest.fn(),
  getFiatWithdrawals: jest.fn(),
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
    onViewDetails,
  }: {
    transactions: Transaction[];
    isLoading: boolean;
    onViewDetails: (transaction: Transaction) => void;
  }) => (
    <div data-testid="transaction-history-tabs">
      {isLoading ? (
        <div>Loading...</div>
      ) : (
        <div>
          {transactions.map((tx) => (
            <div key={tx.id} onClick={() => onViewDetails(tx)}>
              {tx.id}
            </div>
          ))}
        </div>
      )}
    </div>
  ),
}));

describe("FiatTransactionHistoryPage", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  const mockToast = {
    toast: jest.fn(),
  };

  const mockTranslations = {
    "wallet.history.title": "Transaction History",
    "deposit.refreshHistory": "Refresh History",
    "deposit.errorFetchingHistory": "Failed to load deposit history",
    "deposit.somethingWentWrong": "Something went wrong",
    "withdrawal.errorFetchingHistory": "Failed to load withdrawal history",
    "withdrawal.somethingWentWrong": "Something went wrong",
  };

  const mockDeposit: FiatTransaction = {
    id: "dep1",
    fiat_amount: 100,
    currency: "usd",
    status: "completed",
    created_at: "2024-03-20T10:00:00Z",
    coin_currency: "usd",
    hash: "",
    updated_at: "2024-03-20T10:00:00Z",
    trade_id: 123,
  };

  const mockWithdrawal: FiatTransaction = {
    id: "with1",
    fiat_amount: 50,
    currency: "usd",
    status: "pending",
    created_at: "2024-03-20T11:00:00Z",
    coin_currency: "usd",
    hash: "",
    updated_at: "2024-03-20T11:00:00Z",
    trade_id: 456,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useParams as jest.Mock).mockReturnValue({ currency: "usd", locale: "en" });
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useTranslations as jest.Mock).mockImplementation(
      () => (key: string) =>
        mockTranslations[key as keyof typeof mockTranslations],
    );
    (useToast as jest.Mock).mockReturnValue(mockToast);
    (getFiatDeposits as jest.Mock).mockResolvedValue([mockDeposit]);
    (getFiatWithdrawals as jest.Mock).mockResolvedValue([mockWithdrawal]);
  });

  it("renders the page with loading state", async () => {
    render(<FiatTransactionHistoryPage />);

    expect(screen.getByTestId("protected-layout")).toBeInTheDocument();
    expect(screen.getByText("Transaction History - USD")).toBeInTheDocument();
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("loads and displays transactions successfully", async () => {
    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText("deposit_dep1")).toBeInTheDocument();
      expect(screen.getByText("withdrawal_with1")).toBeInTheDocument();
    });

    expect(getFiatDeposits).toHaveBeenCalled();
    expect(getFiatWithdrawals).toHaveBeenCalled();
  });

  it("handles deposit fetch error", async () => {
    const error = new AxiosError();
    error.response = { data: { message: "API Error" } } as AxiosResponse;
    (getFiatDeposits as jest.Mock).mockRejectedValue(error);

    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(mockToast.toast).toHaveBeenCalledWith({
        title: "Failed to load deposit history",
        description: "API Error",
        variant: "destructive",
      });
    });
  });

  it("handles withdrawal fetch error", async () => {
    const error = new AxiosError();
    error.response = { data: { message: "API Error" } } as AxiosResponse;
    (getFiatWithdrawals as jest.Mock).mockRejectedValue(error);

    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(mockToast.toast).toHaveBeenCalledWith({
        title: "Failed to load withdrawal history",
        description: "API Error",
        variant: "destructive",
      });
    });
  });

  it("handles refresh button click", async () => {
    render(<FiatTransactionHistoryPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText("deposit_dep1")).toBeInTheDocument();
      expect(screen.getByText("withdrawal_with1")).toBeInTheDocument();
    });

    const refreshButton = screen.getByText("Refresh History");
    fireEvent.click(refreshButton);

    // Wait for the refresh to complete
    await waitFor(
      () => {
        expect(getFiatDeposits).toHaveBeenCalledTimes(2);
        expect(getFiatWithdrawals).toHaveBeenCalledTimes(2);
      },
      { timeout: 2000 },
    );
  });

  it("handles view details click for deposit", async () => {
    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText("deposit_dep1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("deposit_dep1"));

    expect(mockRouter.push).toHaveBeenCalledWith("/en/trade/123");
  });

  it("handles view details click for withdrawal", async () => {
    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText("withdrawal_with1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText("withdrawal_with1"));

    expect(mockRouter.push).toHaveBeenCalledWith("/en/trade/456");
  });

  it("applies correct status classes", () => {
    render(<FiatTransactionHistoryPage />);
    expect(screen.getByTestId("transaction-history-tabs")).toBeInTheDocument();
  });

  it("filters transactions by currency", async () => {
    const eurDeposit = { ...mockDeposit, currency: "eur" };
    (getFiatDeposits as jest.Mock).mockResolvedValue([mockDeposit, eurDeposit]);

    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText("deposit_dep1")).toBeInTheDocument();
      expect(screen.queryByText("deposit_eur")).not.toBeInTheDocument();
    });
  });

  it("handles non-array response from deposit API", async () => {
    (getFiatDeposits as jest.Mock).mockResolvedValue("not an array");
    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.queryByText("deposit_dep1")).not.toBeInTheDocument();
    });
  });

  it("handles non-array response from withdrawal API", async () => {
    (getFiatWithdrawals as jest.Mock).mockResolvedValue("not an array");
    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(screen.queryByText("withdrawal_with1")).not.toBeInTheDocument();
    });
  });

  it("handles non-axios error in deposit fetch", async () => {
    const error = new Error("Generic error");
    (getFiatDeposits as jest.Mock).mockRejectedValue(error);

    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(mockToast.toast).toHaveBeenCalledWith({
        title: "Failed to load deposit history",
        description: "Something went wrong",
        variant: "destructive",
      });
    });
  });

  it("handles non-axios error in withdrawal fetch", async () => {
    const error = new Error("Generic error");
    (getFiatWithdrawals as jest.Mock).mockRejectedValue(error);

    render(<FiatTransactionHistoryPage />);

    await waitFor(() => {
      expect(mockToast.toast).toHaveBeenCalledWith({
        title: "Failed to load withdrawal history",
        description: "Something went wrong",
        variant: "destructive",
      });
    });
  });
});
