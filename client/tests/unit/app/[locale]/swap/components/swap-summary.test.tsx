import { render, screen, fireEvent } from "@testing-library/react";
import { SwapSummary } from "@/app/[locale]/swap/components/swap-summary";
import { BigNumber } from "bignumber.js";

// Mock the next-intl translations
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

describe("SwapSummary", () => {
  const defaultProps = {
    exchangeRate: new BigNumber(1.5),
    outputToken: "ETH",
    inputToken: "USDT",
    disabled: false,
    onSwap: jest.fn(),
    buttonText: "Swap",
    priceImpact: 2.5,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with default props", () => {
    render(<SwapSummary {...defaultProps} />);

    // Check if exchange rate is rendered
    expect(screen.getByText("exchangeRate")).toBeInTheDocument();
    expect(screen.getByText("1 USDT = 1.5 ETH")).toBeInTheDocument();

    // Check if price impact is rendered
    expect(screen.getByText("priceImpact")).toBeInTheDocument();
    expect(screen.getByText("2.50%")).toBeInTheDocument();

    // Check if button is rendered
    const button = screen.getByRole("button", { name: "Swap" });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
  });

  it("handles null exchange rate", () => {
    render(<SwapSummary {...defaultProps} exchangeRate={null} />);

    // Exchange rate should not be rendered
    expect(screen.queryByText("exchangeRate")).not.toBeInTheDocument();
    expect(screen.queryByText(/1 USDT =/)).not.toBeInTheDocument();
  });

  it("handles zero exchange rate", () => {
    render(<SwapSummary {...defaultProps} exchangeRate={new BigNumber(0)} />);

    // Exchange rate should not be rendered
    expect(screen.queryByText("exchangeRate")).not.toBeInTheDocument();
    expect(screen.queryByText(/1 USDT =/)).not.toBeInTheDocument();
  });

  it("formats exchange rate with 2 decimal places when rate > 1", () => {
    render(
      <SwapSummary
        {...defaultProps}
        exchangeRate={new BigNumber(123.456789)}
      />,
    );

    expect(screen.getByText("1 USDT = 123.46 ETH")).toBeInTheDocument();
  });

  it("formats exchange rate with 6 decimal places when rate < 1", () => {
    render(
      <SwapSummary
        {...defaultProps}
        exchangeRate={new BigNumber(0.123456789)}
      />,
    );

    expect(screen.getByText("1 USDT = 0.123457 ETH")).toBeInTheDocument();
  });

  it("handles exchange rate that throws on toNumber", () => {
    const mockExchangeRate = new BigNumber("1.5");
    mockExchangeRate.toNumber = jest.fn().mockImplementation(() => {
      throw new Error("Mock error");
    });

    render(<SwapSummary {...defaultProps} exchangeRate={mockExchangeRate} />);

    expect(screen.getByText("1 USDT = 0 ETH")).toBeInTheDocument();
  });

  it("renders error message when provided", () => {
    const errorMessage = "Insufficient balance";
    render(<SwapSummary {...defaultProps} errorMessage={errorMessage} />);

    expect(screen.getByText(errorMessage)).toBeInTheDocument();
  });

  it("applies default button variant for price impact < 5%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={4} />);

    const button = screen.getByRole("button", { name: "Swap" });
    expect(button).toHaveClass("bg-primary");
  });

  it("applies secondary button variant for price impact between 5% and 10%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={7} />);

    const button = screen.getByRole("button", { name: "Swap" });
    expect(button).toHaveClass("bg-secondary");
  });

  it("applies destructive button variant for price impact >= 10%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={12} />);

    const button = screen.getByRole("button", { name: "Swap" });
    expect(button).toHaveClass("bg-destructive");
  });

  it("handles swap button click", () => {
    render(<SwapSummary {...defaultProps} />);

    const button = screen.getByRole("button", { name: "Swap" });
    fireEvent.click(button);

    expect(defaultProps.onSwap).toHaveBeenCalledTimes(1);
  });

  it("renders disabled button", () => {
    render(<SwapSummary {...defaultProps} disabled={true} />);

    const button = screen.getByRole("button", { name: "Swap" });
    expect(button).toBeDisabled();
  });

  it("applies text-amber-600 class for price impact between 5% and 10%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={7} />);

    const priceImpactValue = screen.getByText("7.00%");
    expect(priceImpactValue).toHaveClass("text-amber-600");
  });

  it("applies text-red-600 class for price impact >= 10%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={12} />);

    const priceImpactValue = screen.getByText("12.00%");
    expect(priceImpactValue).toHaveClass("text-red-600");
  });

  it("applies text-muted-foreground class for price impact < 5%", () => {
    render(<SwapSummary {...defaultProps} priceImpact={2} />);

    const priceImpactValue = screen.getByText("2.00%");
    expect(priceImpactValue).toHaveClass("text-muted-foreground");
  });

  it("does not render price impact when it's 0", () => {
    render(<SwapSummary {...defaultProps} priceImpact={0} />);

    expect(screen.queryByText("priceImpact")).not.toBeInTheDocument();
  });
});
