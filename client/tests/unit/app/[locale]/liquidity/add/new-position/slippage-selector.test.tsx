import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { SlippageSelector } from "@/app/[locale]/liquidity/add/new-position/slippage-selector";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

describe("SlippageSelector", () => {
  const mockOnChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with default initialValue", () => {
    render(<SlippageSelector initialValue={100} onChange={mockOnChange} />);

    // Check if title is rendered
    expect(screen.getByText("liquidity.slippageTolerance")).toBeInTheDocument();

    // Check if all options are rendered
    expect(screen.getByText("Auto")).toBeInTheDocument();
    expect(screen.getByText("0.5%")).toBeInTheDocument();
    expect(screen.getByText("1%")).toBeInTheDocument();
    expect(screen.getByText("2%")).toBeInTheDocument();
    expect(screen.getByText("5%")).toBeInTheDocument();

    // Check if default value (Auto) is selected
    const autoButton = screen.getByText("Auto");
    expect(autoButton).toHaveClass("bg-primary");
  });

  it("renders with custom initialValue", () => {
    render(<SlippageSelector initialValue={0.5} onChange={mockOnChange} />);

    // Check if custom value (0.5%) is selected
    const selectedButton = screen.getByText("0.5%");
    expect(selectedButton).toHaveClass("bg-primary");
  });

  it("calls onChange when selecting a different option", () => {
    render(<SlippageSelector initialValue={100} onChange={mockOnChange} />);

    // Click on 2% option
    const optionButton = screen.getByText("2%");
    fireEvent.click(optionButton);

    // Check if onChange was called with correct value
    expect(mockOnChange).toHaveBeenCalledTimes(1);
    expect(mockOnChange).toHaveBeenCalledWith(2);

    // Check if button is now selected
    expect(optionButton).toHaveClass("bg-primary");
  });

  it("updates selected value when initialValue prop changes", () => {
    const { rerender } = render(
      <SlippageSelector initialValue={100} onChange={mockOnChange} />,
    );

    // Initially Auto should be selected
    expect(screen.getByText("Auto")).toHaveClass("bg-primary");

    // Rerender with new initialValue
    rerender(<SlippageSelector initialValue={5} onChange={mockOnChange} />);

    // Now 5% should be selected
    expect(screen.getByText("5%")).toHaveClass("bg-primary");
  });
});
