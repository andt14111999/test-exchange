import * as React from "react";
import { render, screen } from "@testing-library/react";
import { Separator } from "@/components/ui/separator";

describe("Separator", () => {
  it("renders with default horizontal orientation", () => {
    render(<Separator data-testid="separator" />);
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveClass("shrink-0", "bg-border", "h-[1px]", "w-full");
  });

  it("renders with vertical orientation", () => {
    render(<Separator data-testid="separator" orientation="vertical" />);
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveClass("shrink-0", "bg-border", "h-full", "w-[1px]");
  });

  it("applies custom className", () => {
    render(<Separator data-testid="separator" className="custom-class" />);
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveClass("custom-class");
  });

  it("handles decorative attribute correctly", () => {
    render(<Separator data-testid="separator" decorative={false} />);
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveAttribute("data-orientation", "horizontal");
    expect(separator).not.toHaveAttribute("data-disabled");
  });

  it("forwards ref correctly", () => {
    const ref = React.createRef<HTMLDivElement>();
    render(<Separator ref={ref} data-testid="separator" />);
    expect(ref.current).toBe(screen.getByTestId("separator"));
  });

  it("spreads additional props correctly", () => {
    render(
      <Separator
        data-testid="separator"
        data-custom="test"
        aria-label="separator"
      />,
    );
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveAttribute("data-custom", "test");
    expect(separator).toHaveAttribute("aria-label", "separator");
  });
});
