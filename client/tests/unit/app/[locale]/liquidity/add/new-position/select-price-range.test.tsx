import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { SelectPriceRange } from "@/app/[locale]/liquidity/add/new-position/select-price-range";
import { TickMath } from "@/lib/amm/tick-math";
import { formatDisplayPrice } from "@/lib/amm/position-utils";
import { BigNumber } from "bignumber.js";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock TickMath
jest.mock("@/lib/amm/tick-math", () => ({
  TickMath: {
    tickToPrice: jest.fn((tick) => tick * 1.0001),
    priceToTick: jest.fn((price) => Math.log(price) / Math.log(1.0001)),
    roundToTickSpacing: jest.fn(
      (tick, spacing) => Math.round(tick / spacing) * spacing,
    ),
    MIN_TICK: -887272,
  },
}));

// Mock formatDisplayPrice
jest.mock("@/lib/amm/position-utils", () => ({
  formatDisplayPrice: jest.fn((price) => price.toFixed(2)),
}));

describe("SelectPriceRange", () => {
  const mockPool = {
    id: 1,
    pair: "USDT/VND",
    name: "USDT/VND",
    token0: "USDT",
    token1: "VND",
    fee: 0.3,
    tickSpacing: 60,
    currentTick: 1000,
    price: new BigNumber("1.0001"),
    sqrtPriceX96: new BigNumber("1.0001"),
    apr: 5.2,
    liquidity: new BigNumber("1000000"),
  };

  const mockOnTicksChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly with initial values", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    expect(screen.getByText("liquidity.priceRange")).toBeInTheDocument();
    expect(screen.getByText("liquidity.minPrice")).toBeInTheDocument();
    expect(screen.getByText("liquidity.maxPrice")).toBeInTheDocument();
    expect(screen.getByText("Tick: 1000")).toBeInTheDocument();
    expect(screen.getByText("Tick: 2000")).toBeInTheDocument();
  });

  it("handles min price input changes", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "1.5" } });
    expect(minPriceInput).toHaveValue("1.5");

    // Simulate blur event
    fireEvent.blur(minPriceInput);
    expect(mockOnTicksChange).toHaveBeenCalled();
  });

  it("handles max price input changes", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [, maxPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(maxPriceInput, { target: { value: "2.5" } });
    expect(maxPriceInput).toHaveValue("2.5");

    // Simulate blur event
    fireEvent.blur(maxPriceInput);
    expect(mockOnTicksChange).toHaveBeenCalled();
  });

  it("adjusts upper tick when min price is greater than or equal to max price", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "2.5" } });
    fireEvent.blur(minPriceInput);

    expect(mockOnTicksChange).toHaveBeenCalledWith(
      expect.any(Number),
      expect.any(Number),
    );
  });

  it("adjusts lower tick when max price is less than or equal to min price", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [, maxPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(maxPriceInput, { target: { value: "0.5" } });
    fireEvent.blur(maxPriceInput);

    expect(mockOnTicksChange).toHaveBeenCalledWith(
      expect.any(Number),
      expect.any(Number),
    );
  });

  it("handles invalid input values", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "invalid" } });
    fireEvent.blur(minPriceInput);

    expect(mockOnTicksChange).not.toHaveBeenCalled();
  });

  it("handles negative input values", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "-1" } });
    fireEvent.blur(minPriceInput);

    expect(mockOnTicksChange).not.toHaveBeenCalled();
  });

  it("handles zero input values", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "0" } });
    fireEvent.blur(minPriceInput);

    expect(mockOnTicksChange).not.toHaveBeenCalled();
  });

  it("maintains tick spacing when adjusting prices", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    const [minPriceInput] = screen.getAllByPlaceholderText("0.00");
    fireEvent.change(minPriceInput, { target: { value: "1.5" } });
    fireEvent.blur(minPriceInput);

    expect(TickMath.roundToTickSpacing).toHaveBeenCalledWith(
      expect.any(Number),
      mockPool.tickSpacing,
    );
  });

  it("updates display values when ticks change", () => {
    const initialLowerTick = 1000;
    const initialUpperTick = 2000;

    render(
      <SelectPriceRange
        pool={mockPool}
        initialLowerTick={initialLowerTick}
        initialUpperTick={initialUpperTick}
        onTicksChange={mockOnTicksChange}
      />,
    );

    expect(formatDisplayPrice).toHaveBeenCalledWith(expect.any(Number));
  });
});
