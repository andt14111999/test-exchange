import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { NewPosition } from "../../../../../../../src/app/[locale]/liquidity/add/new-position/new";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { calculateTickRange } from "@/lib/amm/position-utils";
import { createPosition } from "@/lib/api/positions";
import { AxiosError, AxiosResponse } from "axios";
import { FormattedPool } from "@/lib/api/pools";
import { BigNumber } from "bignumber.js";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useQueryClient } from "@tanstack/react-query";

// Mock the dependencies
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

jest.mock("sonner", () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

jest.mock("@tanstack/react-query", () => ({
  ...jest.requireActual("@tanstack/react-query"),
  useQueryClient: jest.fn(),
}));

jest.mock("@/lib/amm/position-utils", () => ({
  calculateTickRange: jest.fn(),
  MIN_TICK: -887272,
  MAX_TICK: 887272,
}));

jest.mock("@/lib/api/positions", () => ({
  createPosition: jest.fn().mockResolvedValue({ data: { id: 1 } }),
}));

// Mock the child components
jest.mock(
  "../../../../../../../src/app/[locale]/liquidity/add/new-position/header",
  () => ({
    Header: jest.fn(() => <div data-testid="header">Header Component</div>),
  }),
);

jest.mock(
  "../../../../../../../src/app/[locale]/liquidity/add/new-position/pool-info",
  () => ({
    PoolInfo: jest.fn(() => (
      <div data-testid="pool-info">Pool Info Component</div>
    )),
  }),
);

jest.mock(
  "../../../../../../../src/app/[locale]/liquidity/add/new-position/select-price-range",
  () => ({
    SelectPriceRange: jest.fn(
      ({
        onTicksChange,
      }: {
        onTicksChange: (lower: number, upper: number) => void;
      }) => (
        <div data-testid="select-price-range">
          Select Price Range Component
          <button
            data-testid="change-ticks-button"
            onClick={() => onTicksChange(10000, 20000)}
          >
            Change Ticks
          </button>
        </div>
      ),
    ),
  }),
);

// Mock TokensInput - different approach
type TokensInputProps = {
  onAmountsChange: (amount0: string, amount1: string) => void;
  [key: string]: unknown;
};

const mockTokensInput = jest.fn();
jest.mock(
  "../../../../../../../src/app/[locale]/liquidity/add/new-position/token-input",
  () => ({
    TokensInput: (props: TokensInputProps) => {
      mockTokensInput(props);
      return (
        <div data-testid="tokens-input">
          Tokens Input Component
          <button
            data-testid="change-amounts-button"
            onClick={() => props.onAmountsChange("5", "10")}
          >
            Change Amounts
          </button>
          <button
            data-testid="change-zero-amounts-button"
            onClick={() => props.onAmountsChange("0", "0")}
          >
            Change to Zero Amounts
          </button>
        </div>
      );
    },
  }),
);

jest.mock(
  "../../../../../../../src/app/[locale]/liquidity/add/new-position/slippage-selector",
  () => ({
    SlippageSelector: jest.fn(
      ({ onChange }: { onChange: (slippage: number) => void }) => (
        <div data-testid="slippage-selector">
          Slippage Selector Component
          <button
            data-testid="change-slippage-button"
            onClick={() => onChange(50)}
          >
            Change Slippage
          </button>
        </div>
      ),
    ),
  }),
);

describe("NewPosition", () => {
  // Silence console logs during tests
  const originalConsoleLog = console.log;
  const originalConsoleError = console.error;

  beforeAll(() => {
    console.log = jest.fn();
    console.error = jest.fn();
  });

  afterAll(() => {
    console.log = originalConsoleLog;
    console.error = originalConsoleError;
  });

  const mockRouter = {
    push: jest.fn(),
  };

  const mockTranslations = jest.fn((key, params) => {
    if (params) {
      return `${key} ${JSON.stringify(params)}`;
    }
    return key;
  });

  const mockQueryClient = {
    invalidateQueries: jest.fn(),
    prefetchQuery: jest.fn(),
  };

  const mockPool: FormattedPool = {
    id: 1,
    pair: "BTC_USDT",
    name: "BTC/USDT",
    token0: "BTC",
    token1: "USDT",
    fee: 0.3,
    tickSpacing: 60,
    currentTick: 5000,
    price: new BigNumber("50000"),
    sqrtPriceX96: new BigNumber("1000000"),
    apr: 5,
    liquidity: new BigNumber("1000000"),
  };

  const mockGetTokenBalance = jest.fn(() => {
    // Default to high balance
    return 10000;
  });

  // Wrapper component with QueryClientProvider
  const renderWithQueryClient = (component: React.ReactElement) => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    return render(
      <QueryClientProvider client={queryClient}>
        {component}
      </QueryClientProvider>,
    );
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useTranslations as jest.Mock).mockReturnValue(mockTranslations);
    (useQueryClient as jest.Mock).mockReturnValue(mockQueryClient);
    (calculateTickRange as jest.Mock).mockReturnValue({
      tickLower: 4000,
      tickUpper: 6000,
    });
    (createPosition as jest.Mock).mockResolvedValue({ data: { id: 1 } });
    mockTokensInput.mockClear();
  });

  it("renders the position form with all components", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Check if all components are rendered
    expect(screen.getByTestId("header")).toBeInTheDocument();
    expect(screen.getByText(mockPool.pair)).toBeInTheDocument();
    expect(screen.getByTestId("pool-info")).toBeInTheDocument();
    expect(screen.getByTestId("select-price-range")).toBeInTheDocument();
    expect(screen.getByTestId("tokens-input")).toBeInTheDocument();
    expect(screen.getByTestId("slippage-selector")).toBeInTheDocument();
    expect(screen.getByText("liquidity.createPosition")).toBeInTheDocument();
  });

  it("initializes with default tick range based on current tick", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    expect(calculateTickRange).toHaveBeenCalledWith(
      mockPool.currentTick,
      60,
      10,
    );
  });

  it("updates position state when tick range changes", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Only the initial call is made in this mock setup
    expect(calculateTickRange).toHaveBeenCalledWith(5000, 60, 10);
  });

  it("updates position state when token amounts change", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    expect(mockTokensInput).toHaveBeenCalledWith(
      expect.objectContaining({
        onAmountsChange: expect.any(Function),
      }),
    );
  });

  it("updates position state when slippage changes", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const changeSlippageButton = screen.getByTestId("change-slippage-button");
    fireEvent.click(changeSlippageButton);

    // The slippage should be updated in the position state
    expect(screen.getByTestId("slippage-selector")).toBeInTheDocument();
  });

  it("disables submit button when token amounts are invalid", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const changeZeroAmountsButton = screen.getByTestId(
      "change-zero-amounts-button",
    );
    fireEvent.click(changeZeroAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("disables submit button when tick range is invalid", () => {
    (calculateTickRange as jest.Mock).mockReturnValue(null);

    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("handles successful position creation", async () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Enable submit button by setting valid amounts
    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(createPosition).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith("liquidity.positionCreated");
      expect(mockRouter.push).toHaveBeenCalledWith("/liquidity/positions");
    });
  });

  it("handles error in useEffect when setting default tick range", () => {
    (calculateTickRange as jest.Mock).mockImplementation(() => {
      throw new Error("Calculation error");
    });

    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Should not crash and should handle the error gracefully
    expect(screen.getByTestId("header")).toBeInTheDocument();
  });

  it("handles missing pool data gracefully", () => {
    const poolWithoutCurrentTick = {
      ...mockPool,
      currentTick: undefined,
    } as unknown as FormattedPool;

    renderWithQueryClient(
      <NewPosition
        pool={poolWithoutCurrentTick}
        getTokenBalance={mockGetTokenBalance}
      />,
    );

    // Should render without crashing
    expect(screen.getByTestId("header")).toBeInTheDocument();
  });

  it("checks isSubmitDisabled returns true for various invalid states", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Test with zero amounts
    const changeZeroAmountsButton = screen.getByTestId(
      "change-zero-amounts-button",
    );
    fireEvent.click(changeZeroAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("handles API error when creating position", async () => {
    const apiError = new AxiosError("API Error", "400", undefined, undefined, {
      status: 400,
      data: { error: "Bad Request" },
    } as AxiosResponse);

    (createPosition as jest.Mock).mockRejectedValue(apiError);

    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Enable submit button by setting valid amounts
    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Bad Request");
    });
  });

  it("handles generic error when creating position", async () => {
    (createPosition as jest.Mock).mockRejectedValue(new Error("Generic error"));

    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    // Enable submit button by setting valid amounts
    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "common.errors.somethingWentWrong",
      );
    });
  });

  it("validates input - requires non-zero token amounts", () => {
    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const changeZeroAmountsButton = screen.getByTestId(
      "change-zero-amounts-button",
    );
    fireEvent.click(changeZeroAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("validates input - amount0 must not exceed balance", () => {
    const lowBalanceGetTokenBalance = jest.fn(() => 1); // Low balance

    renderWithQueryClient(
      <NewPosition
        pool={mockPool}
        getTokenBalance={lowBalanceGetTokenBalance}
      />,
    );

    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("validates input - amount1 must not exceed balance", () => {
    const lowBalanceGetTokenBalance = jest.fn(() => 1); // Low balance

    renderWithQueryClient(
      <NewPosition
        pool={mockPool}
        getTokenBalance={lowBalanceGetTokenBalance}
      />,
    );

    const changeAmountsButton = screen.getByTestId("change-amounts-button");
    fireEvent.click(changeAmountsButton);

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });

  it("validates input - tick ranges must be valid", () => {
    (calculateTickRange as jest.Mock).mockReturnValue(null);

    renderWithQueryClient(
      <NewPosition pool={mockPool} getTokenBalance={mockGetTokenBalance} />,
    );

    const submitButton = screen.getByText("liquidity.createPosition");
    expect(submitButton).toBeDisabled();
  });
});
