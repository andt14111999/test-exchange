import { render, screen } from "@testing-library/react";
import { TokenAmount } from "@/components/amm/token-amount";
import { BigNumber } from "bignumber.js";

interface FormatNumberOptions {
  decimals?: number;
  currency?: string;
  showSymbol?: boolean;
}

// Mock the number format provider
jest.mock("@/components/providers/number-format-provider", () => ({
  useNumberFormat: () => ({
    isHydrated: true,
    locale: "en-US",
    formatNumber: (
      value: string | number | BigNumber,
      options: FormatNumberOptions = {},
    ) => {
      const { decimals = 2, currency, showSymbol = true } = options;
      const numValue = new BigNumber(value).toNumber();
      const formatter = new Intl.NumberFormat("en-US", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      });
      const formattedNumber = formatter.format(numValue);
      const symbols: Record<string, string> = {
        VND: "₫",
        PHP: "₱",
        NGN: "₦",
        USDT: "$",
      };
      const symbol = currency && showSymbol ? symbols[currency] || "" : "";
      return `${symbol}${formattedNumber}`;
    },
  }),
}));

describe("TokenAmount", () => {
  it("renders with default props", () => {
    render(<TokenAmount amount="1234.5678" token="USDT" />);
    expect(screen.getByText("$1,234.57 USDT")).toBeInTheDocument();
  });

  it("renders with custom decimals", () => {
    render(<TokenAmount amount="1234.5678" token="USDT" decimals={4} />);
    expect(screen.getByText("$1,234.5678 USDT")).toBeInTheDocument();
  });

  it("renders with BigNumber amount", () => {
    render(<TokenAmount amount={new BigNumber("1234.5678")} token="USDT" />);
    expect(screen.getByText("$1,234.57 USDT")).toBeInTheDocument();
  });

  it("renders with number amount", () => {
    render(<TokenAmount amount={1234.5678} token="USDT" />);
    expect(screen.getByText("$1,234.57 USDT")).toBeInTheDocument();
  });

  it("renders with custom className", () => {
    const { container } = render(
      <TokenAmount amount="1234.5678" token="USDT" className="text-red-500" />,
    );
    expect(container.querySelector(".text-red-500")).toBeInTheDocument();
  });

  it("renders without symbol when showSymbol is false", () => {
    render(<TokenAmount amount="1234.5678" token="USDT" showSymbol={false} />);
    expect(screen.getByText("1,234.57 USDT")).toBeInTheDocument();
  });

  it("renders with different token symbols", () => {
    const { rerender } = render(<TokenAmount amount="1234.5678" token="VND" />);
    expect(screen.getByText("₫1,234.57 VND")).toBeInTheDocument();

    rerender(<TokenAmount amount="1234.5678" token="PHP" />);
    expect(screen.getByText("₱1,234.57 PHP")).toBeInTheDocument();

    rerender(<TokenAmount amount="1234.5678" token="NGN" />);
    expect(screen.getByText("₦1,234.57 NGN")).toBeInTheDocument();
  });

  it("renders with token without predefined symbol", () => {
    render(<TokenAmount amount="1234.5678" token="BTC" />);
    expect(screen.getByText("1,234.57 BTC")).toBeInTheDocument();
  });

  it("handles zero amount", () => {
    render(<TokenAmount amount="0" token="USDT" />);
    expect(screen.getByText("$0.00 USDT")).toBeInTheDocument();
  });

  it("handles negative amount", () => {
    render(<TokenAmount amount="-1234.5678" token="USDT" />);
    expect(screen.getByText("$-1,234.57 USDT")).toBeInTheDocument();
  });

  it("handles very large numbers", () => {
    render(
      <TokenAmount amount="1234567890123456.789" token="USDT" decimals={2} />,
    );
    expect(
      screen.getByText("$1,234,567,890,123,456.80 USDT"),
    ).toBeInTheDocument();
  });

  it("handles very small numbers", () => {
    render(
      <TokenAmount amount="0.000000123456789" token="USDT" decimals={9} />,
    );
    expect(screen.getByText("$0.000000123 USDT")).toBeInTheDocument();
  });
});
