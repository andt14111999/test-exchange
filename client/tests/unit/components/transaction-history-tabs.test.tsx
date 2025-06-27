import { render, screen } from "@testing-library/react";
import { TransactionHistoryTabs } from "@/components/transaction-history-tabs";
import { Transaction, TRANSACTION_TYPE } from "@/types/transaction";
import userEvent from "@testing-library/user-event";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock TransactionHistory component
jest.mock("@/components/transaction-history", () => ({
  TransactionHistory: jest.fn(({ transactions }) => (
    <div data-testid="transaction-history">
      {transactions.length} transactions
    </div>
  )),
}));

describe("TransactionHistoryTabs", () => {
  const mockTransactions: Transaction[] = [
    {
      id: "1",
      type: TRANSACTION_TYPE.DEPOSIT,
      amount: 100,
      status: "completed",
      created_at: "2024-03-20T10:00:00Z",
      updated_at: "2024-03-20T10:00:00Z",
      coin_currency: "BTC",
      hash: "hash1",
    },
    {
      id: "2",
      type: TRANSACTION_TYPE.WITHDRAWAL,
      amount: 50,
      status: "pending",
      created_at: "2024-03-20T11:00:00Z",
      updated_at: "2024-03-20T11:00:00Z",
      coin_currency: "ETH",
      hash: "hash2",
    },
    {
      id: "3",
      type: TRANSACTION_TYPE.DEPOSIT,
      amount: 75,
      status: "completed",
      created_at: "2024-03-20T12:00:00Z",
      updated_at: "2024-03-20T12:00:00Z",
      coin_currency: "BTC",
      hash: "hash3",
    },
  ];

  const defaultProps = {
    transactions: mockTransactions,
    isLoading: false,
    statusClassFn: (status: string) => `status-${status}`,
    onViewDetails: jest.fn(),
    showViewDetailsButton: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders all tabs correctly", () => {
    render(<TransactionHistoryTabs {...defaultProps} />);

    expect(screen.getByText("wallet.history.all")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.deposit")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.withdraw")).toBeInTheDocument();
  });

  it("shows all transactions in 'all' tab by default", () => {
    render(<TransactionHistoryTabs {...defaultProps} />);

    expect(screen.getByTestId("transaction-history")).toHaveTextContent(
      "3 transactions",
    );
  });

  it("filters deposit transactions correctly", async () => {
    render(<TransactionHistoryTabs {...defaultProps} />);

    const depositTab = screen.getByText("wallet.history.deposit");
    await userEvent.click(depositTab);

    expect(screen.getByTestId("transaction-history")).toHaveTextContent(
      "2 transactions",
    );
  });

  it("filters withdrawal transactions correctly", async () => {
    render(<TransactionHistoryTabs {...defaultProps} />);

    const withdrawTab = screen.getByText("wallet.history.withdraw");
    await userEvent.click(withdrawTab);

    expect(screen.getByTestId("transaction-history")).toHaveTextContent(
      "1 transactions",
    );
  });

  it("handles empty transactions array", () => {
    render(<TransactionHistoryTabs {...defaultProps} transactions={[]} />);

    expect(screen.getByTestId("transaction-history")).toHaveTextContent(
      "0 transactions",
    );
  });

  it("passes loading state correctly", () => {
    render(<TransactionHistoryTabs {...defaultProps} isLoading={true} />);

    const transactionHistory = screen.getByTestId("transaction-history");
    expect(transactionHistory).toBeInTheDocument();
  });

  it("passes status class function correctly", () => {
    const customStatusClassFn = jest.fn((status: string) => `custom-${status}`);
    render(
      <TransactionHistoryTabs
        {...defaultProps}
        statusClassFn={customStatusClassFn}
      />,
    );

    expect(screen.getByTestId("transaction-history")).toBeInTheDocument();
  });

  it("passes view details handler correctly", () => {
    const customViewDetailsHandler = jest.fn();
    render(
      <TransactionHistoryTabs
        {...defaultProps}
        onViewDetails={customViewDetailsHandler}
      />,
    );

    expect(screen.getByTestId("transaction-history")).toBeInTheDocument();
  });

  it("handles transactions with multiple types correctly", () => {
    const mixedTypeTransaction: Transaction = {
      id: "4",
      type: TRANSACTION_TYPE.DEPOSIT,
      amount: 200,
      status: "completed",
      created_at: "2024-03-20T13:00:00Z",
      updated_at: "2024-03-20T13:00:00Z",
      coin_currency: "BTC",
      hash: "hash4",
    };

    render(
      <TransactionHistoryTabs
        {...defaultProps}
        transactions={[...mockTransactions, mixedTypeTransaction]}
      />,
    );

    expect(screen.getByTestId("transaction-history")).toHaveTextContent(
      "4 transactions",
    );
  });
});
