import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Pools from "@/app/[locale]/liquidity/pools/page";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useAmmPools } from "@/providers/amm-pool-provider";
import { fetchPools } from "@/lib/api/pools";

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock amm-pool-provider
jest.mock("@/providers/amm-pool-provider", () => ({
  useAmmPools: jest.fn(),
}));

// Mock fetchPools
jest.mock("@/lib/api/pools", () => ({
  fetchPools: jest.fn(),
}));

describe("Pools", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  const mockTranslations: Record<string, string> = {
    availablePools: "Available Pools",
    myPositions: "My Positions",
    pool: "Pool",
    apr: "APR",
    feeTier: "Fee Tier",
    tvl: "TVL",
    noPoolsAvailable: "No pools available",
    "common.errors.failedToLoad": "Failed to load pools",
  };

  const mockPools = [
    {
      id: "1",
      name: "BTC/USDT",
      pair: "BTC-USDT",
      apr: 5.2,
      fee: 0.003,
      liquidity: { toNumber: () => 1000000 },
    },
    {
      id: "2",
      name: "ETH/USDT",
      pair: "ETH-USDT",
      apr: 4.8,
      fee: 0.003,
      liquidity: { toNumber: () => 2000000 },
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key],
    );
    (useAmmPools as jest.Mock).mockReturnValue({
      getAllPools: () => mockPools,
      updatePool: jest.fn(),
    });
    // Mock console.error to prevent error output in tests
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("renders loading state initially", () => {
    (fetchPools as jest.Mock).mockImplementation(() => new Promise(() => {}));
    render(<Pools />);
    expect(screen.getByTestId("loading-spinner")).toBeInTheDocument();
  });

  it("renders error state when fetch fails", async () => {
    (fetchPools as jest.Mock).mockRejectedValue(new Error("Failed to fetch"));
    render(<Pools />);
    await waitFor(() => {
      expect(
        screen.getByText(mockTranslations["common.errors.failedToLoad"]),
      ).toBeInTheDocument();
    });
  });

  it("renders pools list when data is loaded", async () => {
    (fetchPools as jest.Mock).mockResolvedValue(mockPools);
    render(<Pools />);

    await waitFor(() => {
      expect(
        screen.getByText(mockTranslations.availablePools),
      ).toBeInTheDocument();
      expect(
        screen.getByText(mockTranslations.myPositions),
      ).toBeInTheDocument();
      expect(screen.getByText("BTC/USDT")).toBeInTheDocument();
      expect(screen.getByText("ETH/USDT")).toBeInTheDocument();
      expect(screen.getByText("5.2%")).toBeInTheDocument();
      expect(screen.getByText("4.8%")).toBeInTheDocument();
      expect(screen.getByText("$1,000,000")).toBeInTheDocument();
      expect(screen.getByText("$2,000,000")).toBeInTheDocument();
    });
  });

  it("renders no pools message when list is empty", async () => {
    (fetchPools as jest.Mock).mockResolvedValue([]);
    (useAmmPools as jest.Mock).mockReturnValue({
      getAllPools: () => [],
      updatePool: jest.fn(),
    });
    render(<Pools />);

    await waitFor(() => {
      expect(
        screen.getByText(mockTranslations.noPoolsAvailable),
      ).toBeInTheDocument();
    });
  });

  it("navigates to positions page when My Positions button is clicked", async () => {
    (fetchPools as jest.Mock).mockResolvedValue(mockPools);
    render(<Pools />);

    await waitFor(() => {
      const positionsButton = screen.getByText(mockTranslations.myPositions);
      fireEvent.click(positionsButton);
      expect(mockRouter.push).toHaveBeenCalledWith("/liquidity/positions");
    });
  });

  it("navigates to add liquidity page when pool is clicked", async () => {
    (fetchPools as jest.Mock).mockResolvedValue(mockPools);
    render(<Pools />);

    await waitFor(() => {
      const poolCard = screen.getByText("BTC/USDT").closest(".cursor-pointer");
      fireEvent.click(poolCard!);
      expect(mockRouter.push).toHaveBeenCalledWith(
        "/liquidity/add?pool=BTC-USDT",
      );
    });
  });

  it("formats currency values correctly", async () => {
    (fetchPools as jest.Mock).mockResolvedValue(mockPools);
    render(<Pools />);

    await waitFor(() => {
      expect(screen.getByText("$1,000,000")).toBeInTheDocument();
      expect(screen.getByText("$2,000,000")).toBeInTheDocument();
    });
  });

  it("updates pools in provider when data is fetched", async () => {
    const mockUpdatePool = jest.fn();
    (useAmmPools as jest.Mock).mockReturnValue({
      getAllPools: () => mockPools,
      updatePool: mockUpdatePool,
    });
    (fetchPools as jest.Mock).mockResolvedValue(mockPools);
    render(<Pools />);

    await waitFor(() => {
      expect(mockUpdatePool).toHaveBeenCalledTimes(2);
      expect(mockUpdatePool).toHaveBeenCalledWith(mockPools[0]);
      expect(mockUpdatePool).toHaveBeenCalledWith(mockPools[1]);
    });
  });
});
