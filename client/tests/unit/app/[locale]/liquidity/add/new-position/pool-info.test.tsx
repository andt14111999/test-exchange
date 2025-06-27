import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { PoolInfo } from "@/app/[locale]/liquidity/add/new-position/pool-info";
import { TickMath } from "@/lib/amm/tick-math";
import { BigNumber } from "bignumber.js";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    const translations: Record<string, string> = {
      "liquidity.selectedPool": "Selected Pool",
      "liquidity.fee": "Fee",
      "liquidity.currentTick": "Current Tick",
    };
    return translations[key] || key;
  },
}));

// Mock TickMath
jest.mock("@/lib/amm/tick-math", () => ({
  TickMath: {
    tickToPrice: jest.fn().mockReturnValue(1.5),
    formatPrice: jest.fn().mockReturnValue("1.50000000"),
  },
}));

describe("PoolInfo", () => {
  const mockPool = {
    id: 1,
    pair: "BTC_USDT",
    name: "BTC/USDT",
    token0: "BTC",
    token1: "USDT",
    fee: 0.3,
    tickSpacing: 60,
    currentTick: 1000,
    price: new BigNumber("1.5"),
    sqrtPriceX96: new BigNumber("1.5"),
    apr: 0.05,
    liquidity: new BigNumber("1000000"),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders pool information correctly", () => {
    render(<PoolInfo pool={mockPool} />);

    // Check if pool pair is displayed
    expect(screen.getByText("BTC_USDT")).toBeInTheDocument();

    // Check if fee is displayed
    expect(screen.getByText("Fee: 0.3%")).toBeInTheDocument();

    // Check if current tick is displayed
    expect(screen.getByText("Current Tick: 1000")).toBeInTheDocument();

    // Check if price is displayed correctly
    expect(screen.getByText("BTC/USDT: 1.50000000")).toBeInTheDocument();
  });

  it("handles price inversion correctly", () => {
    render(<PoolInfo pool={mockPool} />);

    // Initial state
    expect(screen.getByText("BTC/USDT: 1.50000000")).toBeInTheDocument();

    // Click the reverse button
    const reverseButton = screen.getByRole("button", { name: "" });
    fireEvent.click(reverseButton);

    // Check if price and pair are inverted
    expect(screen.getByText("USDT/BTC: 0.66666667")).toBeInTheDocument();

    // Click again to revert
    fireEvent.click(reverseButton);
    expect(screen.getByText("BTC/USDT: 1.50000000")).toBeInTheDocument();
  });

  it("calls TickMath methods correctly", () => {
    render(<PoolInfo pool={mockPool} />);

    // Verify TickMath.tickToPrice was called with correct tick
    expect(TickMath.tickToPrice).toHaveBeenCalledWith(1000);

    // Verify TickMath.formatPrice was called with correct price
    expect(TickMath.formatPrice).toHaveBeenCalledWith(1.5);
  });
});
