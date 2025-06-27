import { render, screen, waitFor } from "@testing-library/react";
import AddLiquidity from "../../../../../../src/app/[locale]/liquidity/add/page";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/use-auth";
import { useWallet } from "@/hooks/use-wallet";
import { fetchPoolByPair, FormattedPool } from "@/lib/api/pools";
import React from "react";
import { NewPosition } from "../../../../../../src/app/[locale]/liquidity/add/new-position/new";
import BigNumber from "bignumber.js";

// Mock the dependencies
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

jest.mock("@/hooks/use-auth", () => ({
  useAuth: jest.fn(),
}));

jest.mock("@/hooks/use-wallet", () => ({
  useWallet: jest.fn(),
}));

jest.mock("@/lib/api/pools", () => ({
  fetchPoolByPair: jest.fn(),
}));

jest.mock(
  "../../../../../../src/app/[locale]/liquidity/add/new-position/new",
  () => ({
    NewPosition: jest
      .fn()
      .mockReturnValue(<div data-testid="new-position">New Position</div>),
  }),
);

// Mock window.sessionStorage
const mockSessionStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
};

Object.defineProperty(window, "sessionStorage", {
  value: mockSessionStorage,
  writable: true,
});

// Mock React's useEffect to make it run synchronously but only once
jest.mock("react", () => {
  const originalReact = jest.requireActual("react");
  return {
    ...originalReact,
    // Override useEffect to run callbacks immediately but only once
    useEffect: (
      callback: React.EffectCallback,
      deps?: React.DependencyList,
    ) => {
      originalReact.useLayoutEffect(() => {
        return callback();
      }, deps);
    },
  };
});

// Mock console.error to prevent noise in test output
const originalConsoleError = console.error;
beforeAll(() => {
  console.error = jest.fn();
});

afterAll(() => {
  console.error = originalConsoleError;
});

describe("AddLiquidity", () => {
  const mockRouter = {
    push: jest.fn(),
  };
  const mockTranslations = jest.fn((key) => key);
  const mockSearchParams = {
    get: jest.fn(),
  };
  const mockPool = {
    id: 1,
    pair: "BTC/USDT",
    name: "BTC/USDT",
    token0: "BTC",
    token1: "USDT",
    fee: 0.3,
    tickSpacing: 60,
    currentTick: 0,
    price: new BigNumber("50000"),
    sqrtPriceX96: new BigNumber("1000000000000000000"),
    apr: 5,
    liquidity: new BigNumber("1000000"),
  };
  const mockWalletData = {
    coin_accounts: [
      { coin_currency: "BTC", balance: 1 },
      { coin_currency: "ETH", balance: 2 },
    ],
    fiat_accounts: [
      { currency: "USDT", balance: 1000 },
      { currency: "USD", balance: 1500 },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();

    // Default mocks
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useSearchParams as jest.Mock).mockReturnValue(mockSearchParams);
    (useTranslations as jest.Mock).mockReturnValue(mockTranslations);
    (useAuth as jest.Mock).mockReturnValue({ user: { id: "1" } });
    (useWallet as jest.Mock).mockReturnValue({ data: mockWalletData });
    mockSearchParams.get.mockReturnValue("BTC-USDT");
    (fetchPoolByPair as jest.Mock).mockResolvedValue(mockPool);
  });

  // Due to complexity with mocking React hooks and testing async behavior,
  // we're testing key functionality with simplified expectations

  it("should save pool parameter to sessionStorage", async () => {
    render(<AddLiquidity />);
    await waitFor(() => {
      expect(sessionStorage.setItem).toHaveBeenCalledWith(
        "lastPoolParam",
        "BTC-USDT",
      );
    });
  });

  it("should redirect to pools page if no pool parameter and none in sessionStorage", () => {
    mockSearchParams.get.mockReturnValue(null);
    mockSessionStorage.getItem.mockReturnValue(null);

    render(<AddLiquidity />);
    expect(mockRouter.push).toHaveBeenCalledWith("/liquidity/pools");
  });

  it("should not redirect when user is not authenticated", async () => {
    // Mock the auth hook to return no user
    (useAuth as jest.Mock).mockReturnValue({ user: null });

    render(<AddLiquidity />);

    // Kiểm tra component vẫn render bình thường
    await waitFor(() => {
      expect(screen.getByTestId("new-position")).toBeInTheDocument();
    });
  });

  it("should handle API errors when fetching pool", async () => {
    // Mock a failed API call
    (fetchPoolByPair as jest.Mock).mockRejectedValue(new Error("API error"));

    render(<AddLiquidity />);

    await waitFor(() => {
      expect(
        screen.getByText("common.errors.failedToLoad"),
      ).toBeInTheDocument();
    });

    expect(console.error).toHaveBeenCalled();
  });

  it("should handle null pool data", async () => {
    (fetchPoolByPair as jest.Mock).mockResolvedValue(null);

    render(<AddLiquidity />);

    await waitFor(() => {
      expect(
        screen.getByText("liquidity.noPositionsFound"),
      ).toBeInTheDocument();
    });
  });

  it("should restore from sessionStorage if pool param is lost", async () => {
    // First simulate no pool param in URL
    mockSearchParams.get.mockReturnValue(null);
    // But a previous pool param in sessionStorage
    mockSessionStorage.getItem.mockReturnValue("ETH-USDT");

    render(<AddLiquidity />);

    await waitFor(() => {
      expect(mockRouter.push).toHaveBeenCalledWith(
        "/liquidity/add?pool=ETH-USDT",
      );
    });
  });

  it("should show loading spinner when loading", async () => {
    // Force loading state to be true initially by delaying the API response
    let resolvePromise!: (value: FormattedPool | null) => void;
    const promise = new Promise<FormattedPool | null>((resolve) => {
      resolvePromise = resolve;
    });

    (fetchPoolByPair as jest.Mock).mockReturnValue(promise);

    render(<AddLiquidity />);

    expect(screen.getByTestId("loading-spinner")).toBeInTheDocument();

    // Resolve the promise to complete loading
    resolvePromise(mockPool);
  });

  it("should calculate token balance for coin accounts correctly", async () => {
    render(<AddLiquidity />);

    // Wait for the component to finish rendering and effects to run
    await waitFor(() => {
      expect(fetchPoolByPair).toHaveBeenCalled();
    });

    // After the component renders with the pool data, new position should be rendered with proper balances
    await waitFor(() => {
      expect(screen.getByTestId("new-position")).toBeInTheDocument();
    });

    // Check if the getTokenBalance function calculates correctly by inspecting the mocked NewPosition props
    expect(NewPosition).toHaveBeenCalledTimes(1);
    const mockCall = (NewPosition as jest.Mock).mock.calls[0][0];
    expect(mockCall).toEqual(
      expect.objectContaining({
        pool: mockPool,
        getTokenBalance: expect.any(Function),
      }),
    );

    // Check getTokenBalance functionality by calling it with various tokens
    const getTokenBalance = mockCall.getTokenBalance;
    expect(getTokenBalance("BTC")).toBe(1);
    expect(getTokenBalance("USDT")).toBe(1000);
    expect(getTokenBalance("ETH")).toBe(2);
    expect(getTokenBalance("USD")).toBe(1500);
    expect(getTokenBalance("UNKNOWN")).toBe(0);
  });

  it("should handle null or undefined wallet data in getTokenBalance", async () => {
    // Mock useWallet to return null data
    (useWallet as jest.Mock).mockReturnValue({ data: null });

    render(<AddLiquidity />);

    await waitFor(() => {
      expect(fetchPoolByPair).toHaveBeenCalled();
    });

    const mockCall = (NewPosition as jest.Mock).mock.calls[0][0];
    const getTokenBalance = mockCall.getTokenBalance;

    // Should return 0 when wallet data is null/undefined
    expect(getTokenBalance("BTC")).toBe(0);
  });

  it("should handle errors in getTokenBalance gracefully", async () => {
    // Mock wallet data that will cause an error when accessing properties
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: null,
        fiat_accounts: undefined,
      },
    });

    render(<AddLiquidity />);

    await waitFor(() => {
      expect(fetchPoolByPair).toHaveBeenCalled();
    });

    const mockCall = (NewPosition as jest.Mock).mock.calls[0][0];
    const getTokenBalance = mockCall.getTokenBalance;

    // Should catch error and return 0
    expect(getTokenBalance("BTC")).toBe(0);
    expect(console.error).toHaveBeenCalled();
  });

  it("should navigate to pools page when clicking the select pool button on error", async () => {
    // Đảm bảo trạng thái lỗi để hiển thị Button
    (fetchPoolByPair as jest.Mock).mockResolvedValue(null);
    render(<AddLiquidity />);
    await waitFor(() => {
      expect(
        screen.getByText("liquidity.noPositionsFound"),
      ).toBeInTheDocument();
    });
    // Click vào nút
    const button = screen.getByRole("button", { name: "liquidity.selectPool" });
    button.click();
    expect(mockRouter.push).toHaveBeenCalledWith("/liquidity/pools");
  });
});
