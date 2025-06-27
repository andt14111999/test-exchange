import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { TokensInput } from "@/app/[locale]/liquidity/add/new-position/token-input";
import { useTranslations } from "next-intl";
import {
  calculateTokenAmounts,
  formatNumberWithCommas,
} from "@/lib/amm/position-utils";
import { FormattedPool } from "@/lib/api/pools";
import { BigNumber } from "bignumber.js";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock position-utils
jest.mock("@/lib/amm/position-utils", () => ({
  calculateTokenAmounts: jest.fn(),
  formatNumberWithCommas: jest.fn((value) => value),
}));

describe("TokensInput", () => {
  const mockPool: FormattedPool = {
    id: 1,
    pair: "ETH/USDT",
    name: "ETH/USDT",
    token0: "ETH",
    token1: "USDT",
    fee: 0.3,
    tickSpacing: 60,
    currentTick: 0,
    price: new BigNumber("2000"),
    sqrtPriceX96: new BigNumber("1000"),
    apr: 0.05,
    liquidity: new BigNumber("1000000"),
  };

  const mockProps = {
    pool: mockPool,
    tickLowerIndex: 100,
    tickUpperIndex: 200,
    token0Balance: "10.0",
    token1Balance: "20000.0",
    onAmountsChange: jest.fn(),
  };

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Mock translations
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string, params?: Record<string, string>) => {
        if (key === "liquidity.tokenAmount") return "Token Amount";
        if (key === "liquidity.balance") return "Balance:";
        if (key === "liquidity.max") return "MAX";
        if (key === "common.errors.insufficientBalance") {
          return `Insufficient balance. You have ${params?.balance} ${params?.token}`;
        }
        return key;
      },
    );

    // Mock formatNumberWithCommas
    (formatNumberWithCommas as jest.Mock).mockImplementation((value) => value);
  });

  it("renders correctly with initial state", () => {
    render(<TokensInput {...mockProps} />);

    // Check if both token inputs are rendered
    expect(screen.getByTestId("token0-input")).toBeInTheDocument();
    expect(screen.getByTestId("token1-input")).toBeInTheDocument();
    // Use a function matcher to find both 'Token Amount' labels
    expect(
      screen.getAllByText((content) => content.includes("Token Amount")),
    ).toHaveLength(2);
    expect(
      screen.getByText((content) => content.includes("ETH")),
    ).toBeInTheDocument();
    expect(
      screen.getByText((content) => content.includes("USDT")),
    ).toBeInTheDocument();
    expect(
      screen.getAllByText((content) => content.includes("Balance:")),
    ).toHaveLength(2);
    expect(screen.getAllByText("MAX")).toHaveLength(2);
  });

  it("handles token0 input change correctly", () => {
    render(<TokensInput {...mockProps} />);
    const token0Input = screen.getByTestId("token0-input");

    // Mock calculateTokenAmounts for token0 input
    (calculateTokenAmounts as jest.Mock).mockReturnValue({
      token0: "5.0",
      token1: "10000.0",
    });

    fireEvent.change(token0Input, { target: { value: "5.0" } });

    expect(token0Input).toHaveValue("5.0");
    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("5.0", "10000.0");
  });

  it("handles token1 input change correctly", () => {
    render(<TokensInput {...mockProps} />);
    const token1Input = screen.getByTestId("token1-input");

    // Mock calculateTokenAmounts for token1 input
    (calculateTokenAmounts as jest.Mock).mockReturnValue({
      token0: "5.0",
      token1: "10000.0",
    });

    fireEvent.change(token1Input, { target: { value: "10000.0" } });

    expect(token1Input).toHaveValue("10000.0");
    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("5.0", "10000.0");
  });

  it("handles MAX button click for token0", () => {
    render(<TokensInput {...mockProps} />);
    const maxButtons = screen.getAllByText("MAX");
    const token0MaxButton = maxButtons[0];

    // Mock calculateTokenAmounts for MAX button
    (calculateTokenAmounts as jest.Mock).mockReturnValue({
      token0: "10.0",
      token1: "20000.0",
    });

    fireEvent.click(token0MaxButton);

    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("10.0", "20000.0");
  });

  it("handles MAX button click for token1", () => {
    render(<TokensInput {...mockProps} />);
    const maxButtons = screen.getAllByText("MAX");
    const token1MaxButton = maxButtons[1];

    // Mock calculateTokenAmounts for MAX button
    (calculateTokenAmounts as jest.Mock).mockReturnValue({
      token0: "10.0",
      token1: "20000.0",
    });

    fireEvent.click(token1MaxButton);

    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("10.0", "20000.0");
  });

  it("shows error message when token0 input exceeds balance", () => {
    render(<TokensInput {...mockProps} />);
    const token0Input = screen.getByTestId("token0-input");

    fireEvent.change(token0Input, { target: { value: "20.0" } });

    expect(
      screen.getByText(/Insufficient balance. You have 10.0 ETH/),
    ).toBeInTheDocument();
  });

  it("shows error message when token1 input exceeds balance", () => {
    render(<TokensInput {...mockProps} />);
    const token1Input = screen.getByTestId("token1-input");

    fireEvent.change(token1Input, { target: { value: "30000.0" } });

    expect(
      screen.getByText(/Insufficient balance. You have 20000.0 USDT/),
    ).toBeInTheDocument();
  });

  it("handles empty input correctly", () => {
    render(<TokensInput {...mockProps} />);
    const token0Input = screen.getByTestId("token0-input");
    const token1Input = screen.getByTestId("token1-input");

    // First set some values
    fireEvent.change(token0Input, { target: { value: "5.0" } });
    fireEvent.change(token1Input, { target: { value: "10000.0" } });

    // Then clear the inputs
    fireEvent.change(token0Input, { target: { value: "" } });
    fireEvent.change(token1Input, { target: { value: "" } });

    expect(token0Input).toHaveValue("");
    expect(token1Input).toHaveValue("");
    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("0", "0");
  });

  it("recalculates token1 when price range changes", () => {
    const { rerender } = render(<TokensInput {...mockProps} />);
    const token0Input = screen.getByTestId("token0-input");

    // Set initial value
    fireEvent.change(token0Input, { target: { value: "5.0" } });

    // Mock calculateTokenAmounts for new price range
    (calculateTokenAmounts as jest.Mock).mockReturnValue({
      token0: "5.0",
      token1: "15000.0",
    });

    // Update price range
    rerender(
      <TokensInput {...mockProps} tickLowerIndex={150} tickUpperIndex={250} />,
    );

    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("5.0", "15000.0");
  });

  it("disables MAX buttons when balance is 0", () => {
    render(<TokensInput {...mockProps} token0Balance="0" token1Balance="0" />);

    const maxButtons = screen.getAllByText("MAX");
    expect(maxButtons[0]).toBeDisabled();
    expect(maxButtons[1]).toBeDisabled();
  });

  it("handles invalid input gracefully", () => {
    render(<TokensInput {...mockProps} />);
    const token0Input = screen.getByTestId("token0-input");

    fireEvent.change(token0Input, { target: { value: "invalid" } });

    expect(token0Input).toHaveValue("invalid");
    expect(mockProps.onAmountsChange).toHaveBeenCalledWith("0", "0");
  });
});
