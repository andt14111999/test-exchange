import React, { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { SwapDetailDialog } from "@/app/[locale]/swap/components/swap-detail-dialog";
import { SwapOrder, fetchSwapOrderDetail } from "@/lib/api/amm-orders";

// Mock useAuth
jest.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({ user: { id: 1 } }),
}));

// Mock useAmmOrderChannel
jest.mock("@/hooks/use-amm-order-channel", () => ({
  useAmmOrderChannel: jest.fn(),
}));

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

// Mock Dialog component from Radix UI to simplify testing
jest.mock("@/components/ui/dialog", () => {
  return {
    Dialog: ({ children, open }: { children: ReactNode; open?: boolean }) =>
      open ? <div>{children}</div> : null,
    DialogContent: ({ children }: { children: ReactNode }) => (
      <div data-testid="dialog-content">{children}</div>
    ),
    DialogHeader: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
    DialogTitle: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
    DialogDescription: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
  };
});

// Mock Card components
jest.mock("@/components/ui/card", () => {
  return {
    Card: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    CardContent: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
    CardDescription: ({
      children,
      ...props
    }: {
      children: ReactNode;
      "data-testid"?: string;
    }) => <div {...props}>{children}</div>,
    CardFooter: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
    CardHeader: ({ children }: { children: ReactNode }) => (
      <div>{children}</div>
    ),
    CardTitle: ({
      children,
      ...props
    }: {
      children: ReactNode;
      "data-testid"?: string;
    }) => (
      <div data-testid="token-pair" {...props}>
        {children}
      </div>
    ),
  };
});

// Mock Badge component
jest.mock("@/components/ui/badge", () => ({
  Badge: ({
    children,
    className,
    variant,
  }: {
    children: ReactNode;
    className?: string;
    variant?: string;
  }) => (
    <span
      className={className || ""}
      data-variant={variant}
      data-status={typeof children === "string" ? children.toLowerCase() : ""}
      data-testid="badge-element"
    >
      {children}
    </span>
  ),
}));

// Mock the next-intl translations
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    if (key === "status.pending") return "Pending";
    if (key === "status.processing") return "Processing";
    if (key === "status.success") return "Success";
    if (key === "status.error") return "Error";
    return key;
  },
}));

// Mock date-fns
jest.mock("date-fns", () => ({
  formatDistanceToNow: jest.fn(() => "2 days ago"),
}));

// Mock vi locale
jest.mock("date-fns/locale", () => ({
  vi: {},
}));

// Mock Loader2
jest.mock("lucide-react", () => ({
  Loader2: () => <div data-testid="loading-icon">Loading...</div>,
}));

// Mock formatCurrency for testing error cases
jest.mock("@/lib/utils/format", () => ({
  formatCurrency: jest.fn((value) => {
    // Simulate error for specific test case
    if (value === "error-trigger") {
      throw new Error("Format error");
    }
    return `${value ? value : "0"},000000`;
  }),
}));

// Mock the API call
jest.mock("@/lib/api/amm-orders", () => ({
  ...jest.requireActual("@/lib/api/amm-orders"),
  fetchSwapOrderDetail: jest.fn(),
}));

describe("SwapDetailDialog", () => {
  const mockSwapOrder: SwapOrder = {
    id: 123,
    identifier: "amm_order_2_usdt_vnd_1746618891",
    zero_for_one: true,
    status: "success",
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
  };

  const defaultProps = {
    open: true,
    onOpenChange: jest.fn(),
    swapId: 123,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders loading state initially", async () => {
    // Delay the API response to ensure loading state is visible
    (fetchSwapOrderDetail as jest.Mock).mockImplementation(
      () =>
        new Promise((resolve) => setTimeout(() => resolve(mockSwapOrder), 100)),
    );

    render(<SwapDetailDialog {...defaultProps} />);

    // Check if loading indicator is displayed
    expect(screen.getByTestId("loading-icon")).toBeInTheDocument();
  });

  test("displays data when API call succeeds", async () => {
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue(mockSwapOrder);

    render(<SwapDetailDialog {...defaultProps} />);

    // Wait for token pair to be displayed
    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Verify API was called with correct ID
    expect(fetchSwapOrderDetail).toHaveBeenCalledWith(123);

    // Check that content is displayed correctly
    expect(screen.getByTestId("token-pair")).toHaveTextContent("USDT → VND");
    expect(screen.getByTestId("order-id")).toHaveTextContent("columns.id: 123");
    expect(screen.getByTestId("status-badge")).toBeInTheDocument();
  });

  test("displays error message when API call fails", async () => {
    // Mock API to reject with error
    (fetchSwapOrderDetail as jest.Mock).mockRejectedValue(
      new Error("API Error"),
    );

    render(<SwapDetailDialog {...defaultProps} />);

    // Wait for error message to be displayed
    await waitFor(
      () => {
        expect(screen.getByTestId("error-message")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("error-message")).toHaveTextContent(
      "detailError",
    );
  });

  test("does not fetch when swapId is null", () => {
    render(
      <SwapDetailDialog open={true} onOpenChange={jest.fn()} swapId={null} />,
    );

    expect(fetchSwapOrderDetail).not.toHaveBeenCalled();
  });

  test("formats fees correctly", async () => {
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      fees: { usdt: 2.5, vnd: 1000 },
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("fees")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );
  });

  test("handles null fees", async () => {
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      fees: null as unknown as Record<string, number>,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("fees")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("fees")).toHaveTextContent("noFees");
  });

  test("displays error message in swap detail when present", async () => {
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      error_message: "Transaction failed due to insufficient funds",
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("error-message")).toHaveTextContent(
      "Transaction failed due to insufficient funds",
    );
  });

  test("formats different status types correctly", async () => {
    // Test pending status
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      status: "pending",
    });

    const { rerender } = render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Check if we have the correct status text
    expect(screen.getByText("Pending")).toBeInTheDocument();

    // Test processing status
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      status: "processing",
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByText("Processing")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Test error status
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      status: "error",
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByText("Error")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Test unknown status
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      status: "unknown-status",
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByText("unknown-status")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );
  });

  test("handles invalid timestamp in formatTime", async () => {
    // Clear any previous mocks
    jest.clearAllMocks();

    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      created_at: 0, // Invalid timestamp should return "N/A"
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 2000 },
    );

    // formatTime should return "N/A" for invalid timestamp
    expect(screen.getByTestId("created-time")).toHaveTextContent("N/A");
  });

  test("handles format error in formatAmount", async () => {
    // Clear any previous mocks
    jest.clearAllMocks();

    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      amount_specified: "invalid-amount", // Invalid amount should return "0"
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 2000 },
    );

    // formatAmount should return "0" for invalid amount
    expect(screen.getByTestId("amount-sent")).toHaveTextContent("0");
  });

  test("handles different token pair formats", async () => {
    // Test vnd->usdt case (zero_for_one = false)
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      zero_for_one: false,
    });

    const { rerender } = render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("token-pair")).toHaveTextContent("VND → USDT");

    // Test with different identifier format
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "some_other_format_with_usdt_vnd",
      zero_for_one: true,
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toHaveTextContent(
          "USDT → VND",
        );
      },
      { timeout: 1000 },
    );

    // Test with identifier that uses the includes fallback method
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "different_format_usdt_vnd_12345",
      zero_for_one: true,
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toHaveTextContent(
          "USDT → VND",
        );
      },
      { timeout: 1000 },
    );

    // Test the fallback method with zero_for_one = false
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "different_format_usdt_vnd_12345",
      zero_for_one: false,
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toHaveTextContent(
          "VND → USDT",
        );
      },
      { timeout: 1000 },
    );

    // Test with unknown format
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "unknown_format",
    });

    rerender(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
      },
      { timeout: 1000 },
    );
  });

  test("handles token pair format error", async () => {
    // Simulate an error in formatTokenPair by passing an identifier that will cause an error
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: null,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
  });

  test("tests formatTime error handling", async () => {
    // Mock formatDistanceToNow to throw an error for this test
    const formatDistanceToNowMock = jest.fn(() => {
      throw new Error("Format time error");
    });

    // Access the mock and override the formatDistanceToNow function
    const dateFnsMock = jest.requireMock("date-fns") as {
      formatDistanceToNow: jest.Mock;
    };
    dateFnsMock.formatDistanceToNow = formatDistanceToNowMock;

    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue(mockSwapOrder);

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    expect(screen.getByTestId("created-time")).toHaveTextContent("N/A");
  });

  test("handles dialog opening and closing", async () => {
    // First, check when dialog is opened with a swap ID
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue(mockSwapOrder);
    const onOpenChangeMock = jest.fn();

    const { rerender } = render(
      <SwapDetailDialog
        open={true}
        onOpenChange={onOpenChangeMock}
        swapId={123}
      />,
    );

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Clear the mock call count
    (fetchSwapOrderDetail as jest.Mock).mockClear();

    // Now, close the dialog
    rerender(
      <SwapDetailDialog
        open={false}
        onOpenChange={onOpenChangeMock}
        swapId={123}
      />,
    );

    // Open dialog with no swapId (should not fetch)
    rerender(
      <SwapDetailDialog
        open={true}
        onOpenChange={onOpenChangeMock}
        swapId={null}
      />,
    );

    // Verify fetchSwapOrderDetail was not called after the first render
    expect(fetchSwapOrderDetail).not.toHaveBeenCalled();
  });

  test("tests formatTokenPair error handling when identifier throws exception", async () => {
    // Create a mock identifier that will cause an error when processed
    const errorIdentifier = {
      toString: () => {
        throw new Error("Identifier error");
      },
      includes: () => {
        throw new Error("Includes error");
      },
      split: () => {
        throw new Error("Split error");
      },
    };

    // Mock the API response with our error-causing identifier
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: errorIdentifier as unknown as string,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display N/A when there's an error
    expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
  });

  test("handles case where no token patterns are found in identifier", async () => {
    // Mock the API response with an identifier that doesn't have recognized patterns
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "some_other_currency_pair_123",
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display N/A when no token patterns are found
    expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
  });

  test("handles identifier with valid format but no matching tokens", async () => {
    // Clear any previous mocks
    jest.clearAllMocks();

    // Mock the API response with an identifier that has valid format but no usdt/vnd tokens
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "amm_order_2_other_tokens_1746618891", // Valid format but no usdt/vnd
      zero_for_one: true,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 2000 },
    );

    // Should display N/A when no token patterns are found
    expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
  });

  test("handles usdt_vnd substring in identifier but not as adjacent parts", async () => {
    // This test specifically targets lines 154-157 in the formatTokenPair function
    // Create an identifier with usdt_vnd as substring but not as adjacent parts in the split
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      // This identifier will fail the loop check because usdt and vnd aren't adjacent parts
      // But will pass the includes check
      identifier: "amm_order_2_btc_eth_with_usdt_vnd_as_substring_1746618891",
      zero_for_one: true,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display USDT → VND based on the includes branch when zero_for_one is true
    expect(screen.getByTestId("token-pair")).toHaveTextContent("USDT → VND");
  });

  test("handles null identifier in formatTokenPair", async () => {
    // Mock the API response with a null identifier
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: null,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display N/A when identifier is null
    expect(screen.getByTestId("token-pair")).toHaveTextContent("N/A");
  });

  test("handles specific case where identifier includes usdt_vnd substring", async () => {
    // Mock the API response with an identifier that contains usdt_vnd as substring
    // but won't be found by the for loop logic (foundInParts remains false)
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "other_usdt_vnd_string_without_parts", // This will ensure includes("usdt_vnd") returns true
      zero_for_one: true,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display the USDT → VND format since it matches the includes condition
    expect(screen.getByTestId("token-pair")).toHaveTextContent("USDT → VND");
  });

  test("handles VND to USDT conversion with usdt_vnd substring", async () => {
    // Mock the API response with an identifier that contains usdt_vnd as substring
    // and where zero_for_one is false (VND to USDT direction)
    (fetchSwapOrderDetail as jest.Mock).mockResolvedValue({
      ...mockSwapOrder,
      identifier: "other_usdt_vnd_string_without_parts",
      zero_for_one: false,
    });

    render(<SwapDetailDialog {...defaultProps} />);

    await waitFor(
      () => {
        expect(screen.getByTestId("token-pair")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    // Should display the VND → USDT format since zero_for_one is false
    expect(screen.getByTestId("token-pair")).toHaveTextContent("VND → USDT");
  });
});
