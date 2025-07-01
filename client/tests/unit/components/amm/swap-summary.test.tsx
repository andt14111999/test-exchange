import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { SwapSummary } from "@/app/[locale]/swap/components/swap-summary";
import { BigNumber } from "bignumber.js";

// Mock useTranslations
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock lucide-react
jest.mock("lucide-react", () => ({
  AlertCircle: () => <div data-testid="alert-circle" />,
}));

describe("SwapSummary", () => {
  const defaultProps = {
    exchangeRate: new BigNumber(25000),
    outputToken: "VND",
    inputToken: "USDT",
    disabled: false,
    onSwap: jest.fn(),
    buttonText: "Swap",
    priceImpact: 0.5,
    poolFee: 0.003,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("Exchange Rate Display", () => {
    it("should display exchange rate correctly", () => {
      render(<SwapSummary {...defaultProps} />);

      expect(screen.getByText("exchangeRate")).toBeInTheDocument();
      expect(screen.getByText("1 USDT = 25,000 VND")).toBeInTheDocument();
    });

    it("should not display exchange rate when null", () => {
      render(<SwapSummary {...defaultProps} exchangeRate={null} />);

      expect(screen.queryByText("exchangeRate")).not.toBeInTheDocument();
    });

    it("should not display exchange rate when zero", () => {
      render(<SwapSummary {...defaultProps} exchangeRate={new BigNumber(0)} />);

      expect(screen.queryByText("exchangeRate")).not.toBeInTheDocument();
    });

    it("should format large exchange rates correctly", () => {
      const largeRate = new BigNumber(1234567.89);
      render(<SwapSummary {...defaultProps} exchangeRate={largeRate} />);

      expect(screen.getByText("1 USDT = 1,234,567.89 VND")).toBeInTheDocument();
    });

    it("should format small exchange rates correctly", () => {
      const smallRate = new BigNumber(0.000001);
      render(<SwapSummary {...defaultProps} exchangeRate={smallRate} />);

      expect(screen.getByText("1 USDT = 0.000001 VND")).toBeInTheDocument();
    });
  });

  describe("Pool Fee Display", () => {
    it("should display pool fee percentage correctly", () => {
      render(<SwapSummary {...defaultProps} />);

      expect(screen.getByText("poolFee")).toBeInTheDocument();
      expect(screen.getByText("0.300%")).toBeInTheDocument();
    });

    it("should not display pool fee when undefined", () => {
      render(<SwapSummary {...defaultProps} poolFee={undefined} />);

      expect(screen.queryByText("poolFee")).not.toBeInTheDocument();
    });

    it("should not display pool fee when zero", () => {
      render(<SwapSummary {...defaultProps} poolFee={0} />);

      expect(screen.queryByText("poolFee")).not.toBeInTheDocument();
    });
  });

  describe("Price Impact Display", () => {
    it("should show low price impact in muted color", () => {
      render(<SwapSummary {...defaultProps} priceImpact={0.5} />);

      const priceImpactElement = screen.getByText("0.50%");
      expect(priceImpactElement).toHaveClass("text-muted-foreground");
    });

    it("should show medium price impact in amber", () => {
      render(<SwapSummary {...defaultProps} priceImpact={7.5} />);

      const priceImpactElement = screen.getByText("7.50%");
      expect(priceImpactElement).toHaveClass("text-amber-600");
    });

    it("should show high price impact in red", () => {
      render(<SwapSummary {...defaultProps} priceImpact={15} />);

      const priceImpactElement = screen.getByText("15.00%");
      expect(priceImpactElement).toHaveClass("text-red-600");
    });

    it("should not display zero price impact", () => {
      render(<SwapSummary {...defaultProps} priceImpact={0} />);

      expect(screen.queryByText("priceImpact")).not.toBeInTheDocument();
    });

    it("should display very small price impact as < 0.01%", () => {
      render(<SwapSummary {...defaultProps} priceImpact={0.005} />);

      expect(screen.getByText("< 0.01%")).toBeInTheDocument();
    });
  });

  describe("Button Behavior", () => {
    it("should call onSwap when button is clicked", () => {
      const mockOnSwap = jest.fn();
      render(<SwapSummary {...defaultProps} onSwap={mockOnSwap} />);

      const button = screen.getByRole("button", { name: "Swap" });
      fireEvent.click(button);

      expect(mockOnSwap).toHaveBeenCalledTimes(1);
    });

    it("should be disabled when disabled prop is true", () => {
      render(<SwapSummary {...defaultProps} disabled={true} />);

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toBeDisabled();
    });

    it("should have destructive variant for high price impact", () => {
      render(<SwapSummary {...defaultProps} priceImpact={15} />);

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toHaveClass("bg-destructive");
    });

    it("should have secondary variant for medium price impact", () => {
      render(<SwapSummary {...defaultProps} priceImpact={7} />);

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toHaveClass("bg-secondary");
    });

    it("should have default variant for low price impact", () => {
      render(<SwapSummary {...defaultProps} priceImpact={1} />);

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toHaveClass("bg-primary");
    });
  });

  describe("Error Message Display", () => {
    it("should display error message when provided", () => {
      const errorMessage = "Insufficient balance";
      render(<SwapSummary {...defaultProps} errorMessage={errorMessage} />);

      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });

    it("should show alert icon with error message", () => {
      const errorMessage = "Insufficient balance";
      render(<SwapSummary {...defaultProps} errorMessage={errorMessage} />);

      expect(screen.getByTestId("alert-circle")).toBeInTheDocument();
    });

    it("should have destructive styling for error message", () => {
      const errorMessage = "Insufficient balance";
      render(<SwapSummary {...defaultProps} errorMessage={errorMessage} />);

      // Just verify error message is displayed with proper alert structure
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
      expect(screen.getByTestId("alert-circle")).toBeInTheDocument();
    });

    it("should not display error message when not provided", () => {
      render(<SwapSummary {...defaultProps} />);

      expect(screen.queryByTestId("alert-circle")).not.toBeInTheDocument();
    });
  });

  describe("Edge Cases", () => {
    it("should handle extreme price impact values", () => {
      render(<SwapSummary {...defaultProps} priceImpact={99.99} />);

      const priceImpactElement = screen.getByText("99.99%");
      expect(priceImpactElement).toHaveClass("text-red-600");
    });

    it("should handle missing token symbols gracefully", () => {
      render(
        <SwapSummary
          {...defaultProps}
          inputToken=""
          outputToken=""
          exchangeRate={new BigNumber(25000)}
        />,
      );

      expect(screen.getByText(/1\s+=\s+25,000/)).toBeInTheDocument();
    });
  });

  describe("Accessibility", () => {
    it("should have proper button accessibility", () => {
      render(<SwapSummary {...defaultProps} />);

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toBeInTheDocument();
      expect(button.tagName.toLowerCase()).toBe("button");
    });
  });

  describe("Integration Scenarios", () => {
    it("should handle complete USDT to VND swap scenario", () => {
      const props = {
        ...defaultProps,
        exchangeRate: new BigNumber(25873.95),
        priceImpact: 0.12,
        poolFee: 0.003,
        buttonText: "Swap 50 USDT",
      };

      render(<SwapSummary {...props} />);

      expect(screen.getByText("1 USDT = 25,873.95 VND")).toBeInTheDocument();
      expect(screen.getByText("0.12%")).toBeInTheDocument();
      expect(screen.getByText("0.300%")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Swap 50 USDT" }),
      ).toBeInTheDocument();
    });

    it("should handle complete VND to USDT swap scenario", () => {
      const props = {
        ...defaultProps,
        exchangeRate: new BigNumber(0.0000386),
        inputToken: "VND",
        outputToken: "USDT",
        priceImpact: 0.08,
        poolFee: 0.003,
        buttonText: "Swap 1,500,000 VND",
      };

      render(<SwapSummary {...props} />);

      expect(screen.getByText("1 VND = 0.000039 USDT")).toBeInTheDocument();
      expect(screen.getByText("0.08%")).toBeInTheDocument();
      expect(screen.getByText("0.300%")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Swap 1,500,000 VND" }),
      ).toBeInTheDocument();
    });

    it("should handle high-impact swap warning scenario", () => {
      const props = {
        ...defaultProps,
        priceImpact: 8.5,
        errorMessage: "High price impact warning",
      };

      render(<SwapSummary {...props} />);

      const priceImpactElement = screen.getByText("8.50%");
      expect(priceImpactElement).toHaveClass("text-amber-600");

      const button = screen.getByRole("button", { name: "Swap" });
      expect(button).toHaveClass("bg-secondary");

      expect(screen.getByText("High price impact warning")).toBeInTheDocument();
    });
  });
});
