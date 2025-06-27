import { render, screen } from "@testing-library/react";
import TransactionHistoryPage from "@/app/[locale]/wallet/history/page";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

// Mock ProtectedLayout component
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({
    children,
    loadingFallback,
  }: {
    children: React.ReactNode;
    loadingFallback: React.ReactNode;
  }) => (
    <div data-testid="protected-layout">
      <div data-testid="loading-fallback">{loadingFallback}</div>
      <div data-testid="content">{children}</div>
    </div>
  ),
}));

describe("TransactionHistoryPage", () => {
  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  it("renders the transaction history page with correct title and description", () => {
    render(<TransactionHistoryPage />);

    expect(screen.getByText("wallet.history.title")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.description")).toBeInTheDocument();
  });

  it("renders the table headers correctly", () => {
    render(<TransactionHistoryPage />);

    expect(screen.getByText("wallet.history.type")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.amount")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.status")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.date")).toBeInTheDocument();
    expect(screen.getByText("wallet.history.hash")).toBeInTheDocument();
  });

  it("renders transaction rows with correct data", () => {
    render(<TransactionHistoryPage />);

    // Check first transaction
    expect(screen.getByText("0.5 BTC")).toBeInTheDocument();
    expect(screen.getByText("0x1234...5678")).toBeInTheDocument();

    // Check second transaction
    expect(screen.getByText("1.2 ETH")).toBeInTheDocument();
    expect(screen.getByText("0x8765...4321")).toBeInTheDocument();
  });

  it("displays correct status badges with appropriate styling", () => {
    render(<TransactionHistoryPage />);

    // Check completed status
    const completedStatus = screen.getAllByText("completed")[0];
    expect(completedStatus).toHaveClass("bg-green-100", "text-green-700");

    // Check pending status
    const pendingStatus = screen.getByText("pending");
    expect(pendingStatus).toHaveClass("bg-yellow-100", "text-yellow-700");

    // Check failed status
    const failedStatus = screen.getByText("failed");
    expect(failedStatus).toHaveClass("bg-yellow-100", "text-red-700");
  });

  it("displays correct transaction type icons", () => {
    render(<TransactionHistoryPage />);

    // Check for deposit icons (ArrowDown)
    const depositIcons = screen.getAllByTestId("arrow-down");
    expect(depositIcons).toHaveLength(2);
    depositIcons.forEach((icon) => {
      expect(icon).toHaveClass("text-green-500");
    });

    // Check for withdraw icons (ArrowUp)
    const withdrawIcons = screen.getAllByTestId("arrow-up");
    expect(withdrawIcons).toHaveLength(2);
    withdrawIcons.forEach((icon) => {
      expect(icon).toHaveClass("text-red-500");
    });
  });

  it("renders loading skeleton when in loading state", () => {
    render(<TransactionHistoryPage />);

    const loadingFallback = screen.getByTestId("loading-fallback");
    expect(loadingFallback).toBeInTheDocument();
    expect(loadingFallback.querySelectorAll(".h-8.w-48")).toHaveLength(1);
    expect(loadingFallback.querySelectorAll(".h-4.w-64")).toHaveLength(1);
    expect(loadingFallback.querySelectorAll(".h-12.w-full")).toHaveLength(3);
  });

  it("formats dates correctly", () => {
    render(<TransactionHistoryPage />);

    const dates = [
      new Date("2024-03-20T10:30:00Z").toLocaleString(),
      new Date("2024-03-19T15:45:00Z").toLocaleString(),
      new Date("2024-03-18T08:15:00Z").toLocaleString(),
      new Date("2024-03-17T20:20:00Z").toLocaleString(),
    ];

    dates.forEach((date) => {
      expect(screen.getByText(date)).toBeInTheDocument();
    });
  });
});
