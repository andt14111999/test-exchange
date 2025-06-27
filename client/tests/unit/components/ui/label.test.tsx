import * as React from "react";
import { render, screen } from "@testing-library/react";
import { Label } from "@/components/ui/label";

describe("Label", () => {
  it("renders with default props", () => {
    render(<Label>Test Label</Label>);
    expect(screen.getByText("Test Label")).toBeInTheDocument();
  });

  it("applies custom className", () => {
    render(<Label className="custom-class">Test Label</Label>);
    const label = screen.getByText("Test Label");
    expect(label).toHaveClass("custom-class");
    expect(label).toHaveClass("text-sm", "font-medium", "leading-none");
  });

  it("forwards ref correctly", () => {
    const ref = React.createRef<HTMLLabelElement>();
    render(<Label ref={ref}>Test Label</Label>);
    expect(ref.current).toBeInstanceOf(HTMLLabelElement);
  });

  it("renders with disabled state", () => {
    render(
      <div>
        <Label htmlFor="test-input">Test Label</Label>
        <input id="test-input" disabled />
      </div>,
    );
    const label = screen.getByText("Test Label");
    expect(label).toHaveClass(
      "peer-disabled:cursor-not-allowed",
      "peer-disabled:opacity-70",
    );
  });

  it("spreads additional props", () => {
    const dataTestId = "test-label";
    render(<Label data-testid={dataTestId}>Test Label</Label>);
    expect(screen.getByTestId(dataTestId)).toBeInTheDocument();
  });
});
