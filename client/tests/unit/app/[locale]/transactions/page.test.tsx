import { render, screen, fireEvent, within } from "@testing-library/react";
import TransactionHistory from "@/app/[locale]/transactions/page";
import { useTrades } from "@/lib/api/hooks/use-trades";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "next/navigation";
import { ApiTrade } from "@/lib/api/trades";

// Mock the hooks
jest.mock("@/lib/api/hooks/use-trades");
jest.mock("@/lib/store/user-store");
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock protected layout
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

const mockTrade: ApiTrade = {
  id: "1",
  ref: "REF001",
  created_at: "2024-03-20T10:00:00Z",
  fiat_amount: "1000",
  fiat_currency: "USD",
  status: "released",
  taker_side: "buy",
  seller: {
    id: "123",
    email: "seller@example.com",
    display_name: "seller_user",
  },
  coin_currency: "BTC",
  coin_amount: "0.05",
  price: "20000",
  payment_method: "bank",
  payment_details: {},
  amount_after_fee: "990",
  coin_trading_fee: "0.001",
  fee_ratio: "0.01",
  updated_at: "2024-03-20T10:00:00Z",
  dispute_reason: undefined,
  dispute_resolution: undefined,
  buyer: {
    id: "456",
    email: "buyer@example.com",
    display_name: "buyer_user",
  },
};

describe("TransactionHistory", () => {
  const mockUseTradesHook = useTrades as jest.Mock;
  const mockUseRouter = useRouter as jest.Mock;
  const mockPush = jest.fn();

  beforeEach(() => {
    mockUseTradesHook.mockReturnValue({
      data: {
        data: [],
        meta: {
          total_pages: 0,
          total_count: 0,
          current_page: 1,
        },
      },
      isLoading: false,
      error: null,
    });

    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: {
        id: "1",
        role: "merchant",
      },
    });

    mockUseRouter.mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      refresh: jest.fn(),
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading state", () => {
    mockUseTradesHook.mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    });

    render(<TransactionHistory />);
    expect(screen.getByTestId("loading-spinner")).toBeInTheDocument();
  });

  it("renders error state", () => {
    mockUseTradesHook.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error("Test error"),
    });

    render(<TransactionHistory />);
    expect(screen.getByText("error")).toBeInTheDocument();
  });

  it("renders empty state", () => {
    render(<TransactionHistory />);
    expect(screen.getByText("noTransactionsFound")).toBeInTheDocument();
  });

  describe("Trade Status Badge", () => {
    const statusMappings = {
      released: "statusCompleted",
      cancelled: "statusCancelled",
      disputed: "statusDisputed",
      paid: "statusPaid",
      awaiting: "statusPending",
      unknown: "UNKNOWN",
    };

    Object.entries(statusMappings).forEach(([status, expectedText]) => {
      it(`renders correct style for ${status} status`, () => {
        mockUseTradesHook.mockReturnValue({
          data: {
            data: [{ ...mockTrade, status }],
            meta: { total_pages: 1, total_count: 1, current_page: 1 },
          },
          isLoading: false,
          error: null,
        });

        render(<TransactionHistory />);
        const statusElement = screen.getByText(expectedText);
        expect(statusElement).toBeInTheDocument();
      });
    });
  });

  describe("Trade Type Tag", () => {
    it("renders BUY tag for merchant when seller ID matches", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: { id: "123", role: "merchant" },
      });

      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 1, total_count: 1, current_page: 1 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      expect(screen.getByText("buy")).toBeInTheDocument();
    });

    it("renders SELL tag for merchant when seller ID doesn't match", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: { id: "456", role: "merchant" },
      });

      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 1, total_count: 1, current_page: 1 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      expect(screen.getByText("sell")).toBeInTheDocument();
    });
  });

  describe("Pagination", () => {
    beforeEach(() => {
      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 3, total_count: 50, current_page: 2 },
        },
        isLoading: false,
        error: null,
      });
    });

    it("disables previous button on first page", () => {
      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 3, total_count: 50, current_page: 1 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      const prevButton = screen.getByRole("button", { name: /previous/i });
      expect(prevButton).toBeDisabled();
    });

    it("disables next button on last page", () => {
      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 3, total_count: 50, current_page: 3 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      const nextButton = screen.getByRole("button", { name: /next/i });
      expect(nextButton).toHaveClass("disabled:opacity-50");
      expect(nextButton).toHaveClass("disabled:pointer-events-none");
    });

    it("handles per page selection change", () => {
      render(<TransactionHistory />);

      const trigger = screen.getByRole("combobox");
      fireEvent.click(trigger);

      const option = screen.getByRole("option", { name: "50" });
      fireEvent.click(option);

      expect(mockUseTradesHook).toHaveBeenCalledWith({
        page: 1,
        per_page: 50,
      });
    });
  });

  describe("Protected Layout Integration", () => {
    it("wraps content in protected layout", () => {
      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 1, total_count: 1, current_page: 1 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      const layout = screen.getByTestId("protected-layout");
      expect(layout).toBeInTheDocument();

      // Check for the main title
      const mainTitle = within(layout).getByRole("heading", { level: 1 });
      expect(mainTitle).toHaveTextContent("title");
    });
  });

  describe("Trade Details Navigation", () => {
    it("navigates to correct trade details page", () => {
      mockUseTradesHook.mockReturnValue({
        data: {
          data: [mockTrade],
          meta: { total_pages: 1, total_count: 1, current_page: 1 },
        },
        isLoading: false,
        error: null,
      });

      render(<TransactionHistory />);
      const viewButton = screen.getByRole("button", { name: /view/i });
      fireEvent.click(viewButton);

      expect(mockPush).toHaveBeenCalledWith("/trade/1");
    });
  });
});
