import * as React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Checkbox } from "@/components/ui/checkbox";

describe("Checkbox", () => {
  it("renders with default styles", () => {
    render(<Checkbox data-testid="checkbox" />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveClass(
      "peer",
      "h-4",
      "w-4",
      "shrink-0",
      "rounded-sm",
      "border",
      "border-primary",
      "ring-offset-background",
    );
  });

  it("applies custom className", () => {
    render(<Checkbox data-testid="checkbox" className="custom-class" />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveClass("custom-class");
  });

  it("renders in checked state", () => {
    render(<Checkbox data-testid="checkbox" checked />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveAttribute("data-state", "checked");
  });

  it("renders in unchecked state", () => {
    render(<Checkbox data-testid="checkbox" />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveAttribute("data-state", "unchecked");
  });

  it("handles click events", () => {
    const onCheckedChange = jest.fn();
    render(
      <Checkbox data-testid="checkbox" onCheckedChange={onCheckedChange} />,
    );
    const checkbox = screen.getByTestId("checkbox");

    fireEvent.click(checkbox);
    expect(onCheckedChange).toHaveBeenCalledWith(true);

    fireEvent.click(checkbox);
    expect(onCheckedChange).toHaveBeenCalledWith(false);
  });

  it("renders disabled state correctly", () => {
    render(<Checkbox data-testid="checkbox" disabled />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveClass(
      "disabled:cursor-not-allowed",
      "disabled:opacity-50",
    );
    expect(checkbox).toBeDisabled();
  });

  it("renders with focus styles", () => {
    render(<Checkbox data-testid="checkbox" />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveClass(
      "focus-visible:outline-none",
      "focus-visible:ring-2",
      "focus-visible:ring-ring",
      "focus-visible:ring-offset-2",
    );
  });

  it("renders check icon when checked", () => {
    render(<Checkbox data-testid="checkbox" checked />);
    const svg = document.querySelector("svg.lucide-check");
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveClass("h-4", "w-4");
  });

  it("forwards ref correctly", () => {
    const ref = React.createRef<HTMLButtonElement>();
    render(<Checkbox ref={ref} data-testid="checkbox" />);
    expect(ref.current).toBeTruthy();
    expect(ref.current).toEqual(screen.getByTestId("checkbox"));
  });

  it("spreads additional props", () => {
    render(<Checkbox data-testid="checkbox" aria-label="Test checkbox" />);
    const checkbox = screen.getByTestId("checkbox");
    expect(checkbox).toHaveAttribute("aria-label", "Test checkbox");
  });
});
