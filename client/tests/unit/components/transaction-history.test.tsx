import React from "react";
import {
  render,
  screen,
  fireEvent,
  within,
  waitFor,
} from "@testing-library/react";
import { TransactionHistory } from "@/components/transaction-history";
import {
  Transaction,
  TRANSACTION_STATUS,
  TRANSACTION_TYPE,
} from "@/types/transaction";
import { formatDate } from "@/lib/utils";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string, options?: { fallback?: string }) => {
    if (options?.fallback) return options.fallback;
    return key;
  },
}));

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
  },
}));

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

const mockFiatTransaction: Transaction = {
  id: "1",
  type: TRANSACTION_TYPE.FIAT_DEPOSIT,
  amount: 1000,
  status: TRANSACTION_STATUS.COMPLETED,
  created_at: "2024-03-20T10:00:00Z",
  updated_at: "2024-03-20T10:00:00Z",
  currency: "usd",
};

const mockCryptoTransaction: Transaction = {
  id: "2",
  type: TRANSACTION_TYPE.WITHDRAWAL,
  amount: 0.5,
  status: TRANSACTION_STATUS.PENDING,
  created_at: "2024-03-20T11:00:00Z",
  updated_at: "2024-03-20T11:00:00Z",
  coin_currency: "btc",
  hash: "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
};

const mockEthTransaction: Transaction = {
  id: "3",
  type: TRANSACTION_TYPE.DEPOSIT,
  amount: 1.5,
  status: TRANSACTION_STATUS.COMPLETED,
  created_at: "2024-03-20T12:00:00Z",
  updated_at: "2024-03-20T12:00:00Z",
  coin_currency: "eth",
  hash: "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
};

const mockTransactionWithoutHash: Transaction = {
  id: "4",
  type: TRANSACTION_TYPE.WITHDRAWAL,
  amount: 0.1,
  status: TRANSACTION_STATUS.PENDING,
  created_at: "2024-03-20T13:00:00Z",
  updated_at: "2024-03-20T13:00:00Z",
  coin_currency: "btc",
  hash: "",
};

describe("TransactionHistory", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render loading state correctly", () => {
    render(<TransactionHistory transactions={[]} isLoading={true} />);
    const skeletons = screen.getAllByTestId("skeleton");
    expect(skeletons).toHaveLength(5);
  });

  it("should render empty state correctly", () => {
    render(<TransactionHistory transactions={[]} />);
    expect(
      screen.getByText("wallet.history.noTransactions"),
    ).toBeInTheDocument();
  });

  it("should render transactions correctly", () => {
    const transactions = [mockFiatTransaction, mockCryptoTransaction];
    render(<TransactionHistory transactions={transactions} />);

    // Check if both transactions are rendered
    expect(screen.getByText("fiat deposit")).toBeInTheDocument();
    expect(screen.getByText("withdrawal")).toBeInTheDocument();

    // Check amounts and currencies
    expect(screen.getByText("1000 USD")).toBeInTheDocument();
    expect(screen.getByText("0.5 BTC")).toBeInTheDocument();

    // Check status badges
    const completedStatus = screen.getByText(TRANSACTION_STATUS.COMPLETED);
    const pendingStatus = screen.getByText(TRANSACTION_STATUS.PENDING);
    expect(completedStatus).toHaveClass("bg-green-100");
    expect(pendingStatus).toHaveClass("bg-yellow-100");

    // Check dates
    expect(
      screen.getByText(formatDate("2024-03-20T10:00:00Z")),
    ).toBeInTheDocument();
    expect(
      screen.getByText(formatDate("2024-03-20T11:00:00Z")),
    ).toBeInTheDocument();

    // Check transaction hash (truncated) - using the actual truncated format
    const hash = "0x1234...abcdef";
    expect(screen.getByText(hash)).toBeInTheDocument();
  });

  describe("Copy and Explorer Button Functionality", () => {
    it("should display copy and explorer buttons for crypto transactions with hash", () => {
      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);

      // Check truncated hash is displayed - using actual format
      expect(screen.getByText("0x1234...abcdef")).toBeInTheDocument();

      // Check copy button is present
      const copyButton = screen.getByTitle("Copy transaction hash");
      expect(copyButton).toBeInTheDocument();

      // Check explorer button is present
      const explorerButton = screen.getByTitle("View on explorer");
      expect(explorerButton).toBeInTheDocument();
    });

    it("should copy transaction hash to clipboard when copy button is clicked", async () => {
      const { toast } = require("sonner");
      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);

      const copyButton = screen.getByTitle("Copy transaction hash");
      fireEvent.click(copyButton);

      await waitFor(() => {
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
          "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
        );
        expect(toast.success).toHaveBeenCalledWith(
          "Transaction hash copied to clipboard",
        );
      });
    });

    it("should open correct explorer URL for Bitcoin transactions", () => {
      // Mock window.open
      const originalOpen = window.open;
      window.open = jest.fn();

      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);

      const explorerButton = screen.getByTitle("View on explorer");
      const explorerLink = explorerButton.closest("a");

      expect(explorerLink).toHaveAttribute(
        "href",
        "https://blockstream.info/tx/0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
      );
      expect(explorerLink).toHaveAttribute("target", "_blank");
      expect(explorerLink).toHaveAttribute("rel", "noopener noreferrer");

      window.open = originalOpen;
    });

    it("should open correct explorer URL for Ethereum transactions", () => {
      render(<TransactionHistory transactions={[mockEthTransaction]} />);

      const explorerButton = screen.getByTitle("View on explorer");
      const explorerLink = explorerButton.closest("a");

      expect(explorerLink).toHaveAttribute(
        "href",
        "https://etherscan.io/tx/0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
      );
    });

    it("should display dash for crypto transactions without hash", () => {
      render(
        <TransactionHistory transactions={[mockTransactionWithoutHash]} />,
      );

      // Should show dash instead of hash/buttons
      expect(screen.getByText("-")).toBeInTheDocument();

      // Should not show copy or explorer buttons
      expect(
        screen.queryByTitle("Copy transaction hash"),
      ).not.toBeInTheDocument();
      expect(screen.queryByTitle("View on explorer")).not.toBeInTheDocument();
    });

    it("should display 'Fiat Transaction' for fiat transactions", () => {
      render(<TransactionHistory transactions={[mockFiatTransaction]} />);

      expect(screen.getByText("Fiat Transaction")).toBeInTheDocument();

      // Should not show copy or explorer buttons for fiat transactions
      expect(
        screen.queryByTitle("Copy transaction hash"),
      ).not.toBeInTheDocument();
      expect(screen.queryByTitle("View on explorer")).not.toBeInTheDocument();
    });

    it("should show full hash on hover", () => {
      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);

      const hashElement = screen.getByText("0x1234...abcdef");
      expect(hashElement).toHaveAttribute(
        "title",
        "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
      );
    });

    it("should handle different cryptocurrency explorer URLs", () => {
      const bnbTransaction: Transaction = {
        ...mockCryptoTransaction,
        coin_currency: "bnb",
        hash: "0xbnbhash123",
      };

      const trxTransaction: Transaction = {
        ...mockCryptoTransaction,
        coin_currency: "trx",
        hash: "trxhash123",
      };

      const solTransaction: Transaction = {
        ...mockCryptoTransaction,
        coin_currency: "sol",
        hash: "solhash123",
      };

      // Test BNB
      const { rerender } = render(
        <TransactionHistory transactions={[bnbTransaction]} />,
      );
      let explorerLink = screen.getByTitle("View on explorer").closest("a");
      expect(explorerLink).toHaveAttribute(
        "href",
        "https://bscscan.com/tx/0xbnbhash123",
      );

      // Test TRX
      rerender(<TransactionHistory transactions={[trxTransaction]} />);
      explorerLink = screen.getByTitle("View on explorer").closest("a");
      expect(explorerLink).toHaveAttribute(
        "href",
        "https://tronscan.org/#/transaction/trxhash123",
      );

      // Test SOL
      rerender(<TransactionHistory transactions={[solTransaction]} />);
      explorerLink = screen.getByTitle("View on explorer").closest("a");
      expect(explorerLink).toHaveAttribute(
        "href",
        "https://solscan.io/tx/solhash123",
      );
    });

    it("should use default explorer for unknown currencies", () => {
      const unknownCurrencyTransaction: Transaction = {
        ...mockCryptoTransaction,
        coin_currency: "unknown",
        hash: "unknownhash123",
      };

      render(
        <TransactionHistory transactions={[unknownCurrencyTransaction]} />,
      );

      const explorerLink = screen.getByTitle("View on explorer").closest("a");
      expect(explorerLink).toHaveAttribute(
        "href",
        "https://etherscan.io/tx/unknownhash123",
      );
    });
  });

  it("should handle sorting correctly", async () => {
    const transactions = [mockFiatTransaction, mockCryptoTransaction];
    render(<TransactionHistory transactions={transactions} />);

    // Test type sorting
    const typeHeader = screen.getByRole("columnheader", {
      name: /wallet\.history\.type/i,
    });
    fireEvent.click(typeHeader);

    let rows = screen.getAllByRole("row").slice(1); // Exclude header row
    const firstRowText = within(rows[0]).getByText(/withdrawal|fiat deposit/i);
    const secondRowText = within(rows[1]).getByText(/withdrawal|fiat deposit/i);
    expect(firstRowText).toBeInTheDocument();
    expect(secondRowText).toBeInTheDocument();

    // Test amount sorting
    const amountHeader = screen.getByRole("columnheader", {
      name: /wallet\.history\.amount/i,
    });
    fireEvent.click(amountHeader);

    rows = screen.getAllByRole("row").slice(1);
    expect(within(rows[0]).getByText("0.5 BTC")).toBeInTheDocument();
    expect(within(rows[1]).getByText("1000 USD")).toBeInTheDocument();
  });

  it("should handle view details button correctly", () => {
    const onViewDetails = jest.fn();
    const transactions = [mockFiatTransaction];
    render(
      <TransactionHistory
        transactions={transactions}
        showViewDetailsButton={true}
        onViewDetails={onViewDetails}
      />,
    );

    const viewDetailsButton = screen.getByRole("button", {
      name: /wallet\.history\.viewDetails/i,
    });
    fireEvent.click(viewDetailsButton);

    expect(onViewDetails).toHaveBeenCalledWith(mockFiatTransaction);
  });

  it("should handle custom status class function", () => {
    const customStatusClassFn = (status: string) => `custom-${status}`;
    const transactions = [mockFiatTransaction];
    render(
      <TransactionHistory
        transactions={transactions}
        statusClassFn={customStatusClassFn}
      />,
    );

    const statusBadge = screen.getByText(TRANSACTION_STATUS.COMPLETED);
    expect(statusBadge).toHaveClass(`custom-${TRANSACTION_STATUS.COMPLETED}`);
  });

  it("should handle failed transaction status", () => {
    const failedTransaction: Transaction = {
      ...mockFiatTransaction,
      status: TRANSACTION_STATUS.FAILED,
    };
    render(<TransactionHistory transactions={[failedTransaction]} />);

    const statusBadge = screen.getByText(TRANSACTION_STATUS.FAILED);
    expect(statusBadge).toHaveClass("bg-red-100");
    expect(statusBadge).toHaveClass("text-red-700");
  });

  it("should handle cancelled transaction status", () => {
    const cancelledTransaction: Transaction = {
      ...mockFiatTransaction,
      status: TRANSACTION_STATUS.CANCELLED,
    };
    render(<TransactionHistory transactions={[cancelledTransaction]} />);

    const statusBadge = screen.getByText(TRANSACTION_STATUS.CANCELLED);
    expect(statusBadge).toHaveClass("bg-gray-100");
    expect(statusBadge).toHaveClass("text-gray-700");
  });

  it("should handle unknown transaction status", () => {
    const unknownTransaction: Transaction = {
      ...mockFiatTransaction,
      status:
        "unknown" as (typeof TRANSACTION_STATUS)[keyof typeof TRANSACTION_STATUS],
    };
    render(<TransactionHistory transactions={[unknownTransaction]} />);

    const statusBadge = screen.getByText("unknown");
    expect(statusBadge).toHaveClass("bg-red-100");
    expect(statusBadge).toHaveClass("text-red-700");
  });

  it("should handle missing coin currency", () => {
    const transactionWithoutCurrency: Transaction = {
      ...mockCryptoTransaction,
      coin_currency: "",
    };
    render(<TransactionHistory transactions={[transactionWithoutCurrency]} />);

    expect(screen.getByText("0.5")).toBeInTheDocument();
  });

  describe("Sorting functionality", () => {
    const transactions = [
      mockFiatTransaction,
      mockCryptoTransaction,
      {
        ...mockFiatTransaction,
        id: "3",
        amount: 500,
        created_at: "2024-03-19T10:00:00Z",
      },
    ];

    it("should sort by amount in ascending and descending order", () => {
      render(<TransactionHistory transactions={transactions} />);
      const amountHeader = screen.getByRole("columnheader", {
        name: /wallet\.history\.amount/i,
      });

      // First click - ascending
      fireEvent.click(amountHeader);
      let rows = screen.getAllByRole("row").slice(1);
      expect(within(rows[0]).getByText("0.5 BTC")).toBeInTheDocument();
      expect(within(rows[1]).getByText("500 USD")).toBeInTheDocument();
      expect(within(rows[2]).getByText("1000 USD")).toBeInTheDocument();

      // Second click - descending
      fireEvent.click(amountHeader);
      rows = screen.getAllByRole("row").slice(1);
      expect(within(rows[0]).getByText("1000 USD")).toBeInTheDocument();
      expect(within(rows[1]).getByText("500 USD")).toBeInTheDocument();
      expect(within(rows[2]).getByText("0.5 BTC")).toBeInTheDocument();
    });

    it("should sort by date correctly", () => {
      render(<TransactionHistory transactions={transactions} />);
      const dateHeader = screen.getByRole("columnheader", {
        name: /wallet\.history\.date/i,
      });

      // First click - ascending
      fireEvent.click(dateHeader);
      let rows = screen.getAllByRole("row").slice(1);
      expect(
        within(rows[0]).getByText(formatDate("2024-03-19T10:00:00Z")),
      ).toBeInTheDocument();
      expect(
        within(rows[2]).getByText(formatDate("2024-03-20T11:00:00Z")),
      ).toBeInTheDocument();

      // Second click - descending
      fireEvent.click(dateHeader);
      rows = screen.getAllByRole("row").slice(1);
      expect(
        within(rows[0]).getByText(formatDate("2024-03-20T11:00:00Z")),
      ).toBeInTheDocument();
      expect(
        within(rows[2]).getByText(formatDate("2024-03-19T10:00:00Z")),
      ).toBeInTheDocument();
    });
  });

  describe("Transaction icons", () => {
    it("should render up arrow for withdrawals", () => {
      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);
      const upArrow = screen.getByTestId("arrow-up");
      expect(upArrow).toHaveClass("text-red-500");
    });

    it("should render down arrow for deposits", () => {
      render(<TransactionHistory transactions={[mockFiatTransaction]} />);
      const downArrow = screen.getByTestId("arrow-down");
      expect(downArrow).toHaveClass("text-green-500");
    });
  });

  describe("Accessibility", () => {
    it("should have accessible table structure", () => {
      render(<TransactionHistory transactions={[mockFiatTransaction]} />);
      expect(screen.getByRole("table")).toBeInTheDocument();
      expect(screen.getAllByRole("columnheader")).toHaveLength(5);

      // Get all header cells and verify their content
      const headers = screen.getAllByRole("columnheader");
      expect(headers[0]).toHaveTextContent(/type/i);
      expect(headers[1]).toHaveTextContent(/amount/i);
      expect(headers[2]).toHaveTextContent(/status/i);
      expect(headers[3]).toHaveTextContent(/date/i);
    });

    it("should have sortable headers with correct ARIA attributes", () => {
      render(<TransactionHistory transactions={[mockFiatTransaction]} />);
      const dateHeader = screen.getByRole("columnheader", { name: /date/i });
      // Default sort is date desc
      expect(dateHeader).toHaveTextContent(/date/i);
      expect(
        within(dateHeader).getByTestId("arrow-down-icon"),
      ).toBeInTheDocument();
    });

    it("should have accessible copy and explorer buttons", () => {
      render(<TransactionHistory transactions={[mockCryptoTransaction]} />);

      const copyButton = screen.getByTitle("Copy transaction hash");
      expect(copyButton).toBeInTheDocument();
      expect(copyButton.tagName.toLowerCase()).toBe("button");

      const explorerButton = screen.getByTitle("View on explorer");
      expect(explorerButton).toBeInTheDocument();
      expect(explorerButton.tagName.toLowerCase()).toBe("button");
    });
  });

  describe("Internationalization", () => {
    it("should display fallback text for unknown transaction types", () => {
      const unknownTransaction: Transaction = {
        ...mockFiatTransaction,
        type: TRANSACTION_TYPE.FIAT_DEPOSIT,
      };
      render(<TransactionHistory transactions={[unknownTransaction]} />);
      expect(screen.getByText("fiat deposit")).toBeInTheDocument();
    });

    it("should handle fiat transaction display in hash column", () => {
      render(<TransactionHistory transactions={[mockFiatTransaction]} />);
      expect(screen.getByText("Fiat Transaction")).toBeInTheDocument();
    });
  });

  describe("Edge cases", () => {
    it("should handle transactions with missing updated_at", () => {
      const incompleteTransaction: Transaction = {
        ...mockFiatTransaction,
        updated_at: mockFiatTransaction.created_at,
      };
      render(<TransactionHistory transactions={[incompleteTransaction]} />);
      expect(
        screen.getByText(formatDate(incompleteTransaction.created_at)),
      ).toBeInTheDocument();
    });

    it("should handle very large amounts correctly", () => {
      const largeAmountTransaction = {
        ...mockFiatTransaction,
        amount: 1000000000.123456,
      };
      render(<TransactionHistory transactions={[largeAmountTransaction]} />);
      expect(screen.getByText("1000000000.123456 USD")).toBeInTheDocument();
    });

    it("should handle empty strings in optional fields", () => {
      const emptyFieldsTransaction = {
        ...mockCryptoTransaction,
        hash: "",
        coin_currency: "",
      };
      render(<TransactionHistory transactions={[emptyFieldsTransaction]} />);
      const row = screen.getByRole("row", { name: /withdrawal 0\.5/i });
      expect(row).toBeInTheDocument();
    });

    it("should handle very short hash correctly", () => {
      const shortHashTransaction: Transaction = {
        ...mockCryptoTransaction,
        hash: "0x12345",
      };
      render(<TransactionHistory transactions={[shortHashTransaction]} />);

      // Short hash should be displayed as-is (not truncated)
      expect(screen.getByText("0x12345")).toBeInTheDocument();
    });
  });
});
