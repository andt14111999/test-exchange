// Mock next-intl/navigation to avoid ESM import errors and provide createNavigation
jest.mock("next-intl/navigation", () => ({
  createNavigation: jest.fn(() => ({
    Link: () => null,
    redirect: jest.fn(),
    usePathname: jest.fn(),
    useRouter: jest.fn(() => ({ push: jest.fn() })),
  })),
}));

// Mock next-intl/server to avoid ESM import errors and provide getRequestConfig
jest.mock("next-intl/server", () => ({
  getRequestConfig: jest.fn(() => () => ({})),
}));

import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { SwapHistory } from "@/app/[locale]/swap/components/swap-history";
import { fetchSwapOrders } from "@/lib/api/amm-orders";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";

// Store the original console.error method
const originalConsoleError = console.error;

// Mock console.error before tests
beforeAll(() => {
  console.error = jest.fn();
});

// Restore console.error after tests
afterAll(() => {
  console.error = originalConsoleError;
});

// Mock the API functions
jest.mock("@/lib/api/amm-orders", () => ({
  ...jest.requireActual("@/lib/api/amm-orders"),
  fetchSwapOrders: jest.fn(),
}));

// Mock formatCurrency
jest.mock("@/lib/utils/format", () => ({
  formatCurrency: jest.fn((value) => {
    if (value === "error-trigger") {
      throw new Error("Format error");
    }
    return `${value},000000`;
  }),
}));

// Mock formatDistanceToNow for date formatting
jest.mock("date-fns", () => ({
  formatDistanceToNow: jest.fn(() => "a few seconds ago"),
}));

// Mock vi locale
jest.mock("date-fns/locale", () => ({
  vi: {},
}));

// Mock next-intl to provide useTranslations with expected English text
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => {
    const map: Record<string, string> = {
      // Full i18n keys
      "swap.history.title": "Swap History",
      "swap.history.loading": "Loading...",
      "swap.history.noOrders": "No swap orders found",
      "swap.history.status.pending": "Pending",
      "swap.history.status.processing": "Processing",
      "swap.history.status.success": "Success",
      "swap.history.status.error": "Error",
      "swap.history.error": "Error loading swap history",
      "swap.history.retry": "Retry",
      "swap.history.statusFilters.all": "All",
      "swap.history.statusFilters.pending": "Pending",
      "swap.history.statusFilters.processing": "Processing",
      "swap.history.statusFilters.success": "Success",
      "swap.history.statusFilters.error": "Error",
      // Short keys and direct labels
      title: "Swap History",
      loading: "Loading...",
      noOrders: "No swap orders found",
      Pending: "Pending",
      Processing: "Processing",
      Success: "Success",
      Error: "Error",
      error: "Error loading swap history",
      retry: "Retry",
      All: "All",
      Next: "Next",
      Previous: "Previous",
      // Add statusFilters and pagination keys
      "statusFilters.pending": "Pending",
      "statusFilters.processing": "Processing",
      "statusFilters.success": "Success",
      "statusFilters.error": "Error",
      "statusFilters.all": "All",
      "pagination.next": "Next",
      "pagination.previous": "Previous",
    };
    return map[key] || key;
  }),
}));

// Mock SwapDetailDialog
jest.mock("@/app/[locale]/swap/components/swap-detail-dialog", () => ({
  SwapDetailDialog: jest.fn(({ open, swapId }) => (
    <div
      data-testid="swap-detail-dialog"
      data-open={open}
      data-swap-id={swapId}
    >
      Dialog Mock
    </div>
  )),
}));

// Create a test data
const mockSwapOrders = [
  {
    id: 1,
    identifier: "amm_order_2_usdt_vnd_1746618891",
    zero_for_one: true,
    status: "pending",
    error_message: null,
    before_tick_index: 100,
    after_tick_index: 102,
    amount_specified: "1000",
    amount_estimated: "900",
    amount_actual: "950",
    amount_received: "950",
    slippage: "0.01",
    fees: { usdt: 2.5 },
    created_at: 1618618891,
    updated_at: 1618618991,
  },
  {
    id: 2,
    identifier: "amm_order_2_usdt_vnd_1746618892",
    zero_for_one: false,
    status: "success",
    error_message: null,
    before_tick_index: 100,
    after_tick_index: 102,
    amount_specified: "500",
    amount_estimated: "400",
    amount_actual: "450",
    amount_received: "450",
    slippage: "0.01",
    fees: { vnd: 1000 },
    created_at: 1618618892,
    updated_at: 1618618992,
  },
];

describe("SwapHistory", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    jest.clearAllMocks();

    // Setup mock response for API call
    (fetchSwapOrders as jest.Mock).mockResolvedValue({
      amm_orders: mockSwapOrders,
      meta: {
        current_page: 1,
        next_page: 2,
        total_pages: 3,
        per_page: 10,
      },
    });

    // Create a new QueryClient for each test
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
  });

  afterEach(() => {
    queryClient.clear();
    jest.clearAllMocks();
  });

  it("renders correctly with loading state", async () => {
    // Initially don't resolve the API call
    (fetchSwapOrders as jest.Mock).mockImplementationOnce(
      () => new Promise(() => {}),
    );

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Check loading state
    expect(screen.getByText("Loading...")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Swap History" }),
    ).toBeInTheDocument();
  });

  it("displays swap orders after loading", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for data to be displayed
    await waitFor(() => {
      expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
    });

    // Check if orders are displayed - using getAllByText for elements that appear multiple times
    expect(screen.getByText("USDT → VND")).toBeInTheDocument();
    expect(screen.getByText("VND → USDT")).toBeInTheDocument();

    // Use getAllByText for elements that appear multiple times and check length
    const amount1000Elements = screen.getAllByText("1000,000000");
    const amount500Elements = screen.getAllByText("500,000000");
    expect(amount1000Elements.length).toBeGreaterThan(0);
    expect(amount500Elements.length).toBeGreaterThan(0);
  });

  it("shows empty state when no orders", async () => {
    // Mock empty response
    (fetchSwapOrders as jest.Mock).mockResolvedValueOnce({
      amm_orders: [],
      meta: {
        current_page: 1,
        next_page: null,
        total_pages: 0,
        per_page: 10,
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for empty state to be displayed
    await waitFor(() => {
      expect(screen.getByText("No swap orders found")).toBeInTheDocument();
    });
  });

  it("handles status filter change", async () => {
    // Create user event instance
    const user = userEvent.setup();

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for data to load
    await waitFor(() => {
      expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
    });

    // Check initial API call
    expect(fetchSwapOrders).toHaveBeenCalledWith(1, 10, "all");

    // Clear previous calls to start fresh
    jest.clearAllMocks();

    // Find and click the Pending tab by text content
    const pendingTab = screen.getByRole("tab", { name: "Pending" });
    await user.click(pendingTab);

    // Verify the API call after clicking
    await waitFor(() => {
      expect(fetchSwapOrders).toHaveBeenCalledWith(1, 10, "pending");
    });
  });

  it("handles pagination correctly", async () => {
    // Create user event instance
    const user = userEvent.setup();

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for data to load
    await waitFor(() => {
      expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
    });

    // Clear previous calls to start fresh
    jest.clearAllMocks();

    // Find pagination elements by their text
    const nextPageButton = screen.getByText("Next");
    await user.click(nextPageButton);

    // Check that API was called with page 2
    await waitFor(() => {
      expect(fetchSwapOrders).toHaveBeenCalledWith(2, 10, "all");
    });

    // Clear previous calls again
    jest.clearAllMocks();

    // Now click a status tab to ensure we reset to page 1
    const successTab = screen.getByRole("tab", { name: "Success" });
    await user.click(successTab);

    // Verify page resets to 1 with new status
    await waitFor(() => {
      expect(fetchSwapOrders).toHaveBeenCalledWith(1, 10, "success");
    });
  });

  it("handles row click to open detail dialog", async () => {
    // Create user event instance
    const user = userEvent.setup();

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for data to load
    await waitFor(() => {
      expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
    });

    // Find first row and click it
    const firstRow = screen.getByText("1").closest("tr");
    expect(firstRow).not.toBeNull();

    if (firstRow) {
      await user.click(firstRow);
    }

    // Check if dialog appears with correct id
    const dialog = screen.getByTestId("swap-detail-dialog");
    expect(dialog).toBeInTheDocument();
    expect(dialog.getAttribute("data-swap-id")).toBe("1");
    expect(dialog.getAttribute("data-open")).toBe("true");
  });

  it("handles error state", async () => {
    // Create user event instance
    const user = userEvent.setup();

    // Mock API to reject with error
    (fetchSwapOrders as jest.Mock).mockRejectedValueOnce(
      new Error("API Error"),
    );

    render(
      <QueryClientProvider client={queryClient}>
        <SwapHistory />
      </QueryClientProvider>,
    );

    // Wait for error state
    await waitFor(() => {
      expect(
        screen.getByText("Error loading swap history"),
      ).toBeInTheDocument();
    });

    // Clear mock calls
    jest.clearAllMocks();

    // Check retry button and click it
    const retryButton = screen.getByText("Retry");
    await user.click(retryButton);

    // Verify refetch was called
    expect(fetchSwapOrders).toHaveBeenCalled();
  });
});
