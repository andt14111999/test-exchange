import * as React from "react";
import { render, screen } from "@testing-library/react";
import { Skeleton } from "@/components/ui/skeleton";

describe("Skeleton", () => {
  it("renders with default styles", () => {
    render(<Skeleton data-testid="skeleton" />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveClass(
      "animate-pulse",
      "rounded-md",
      "bg-muted",
      "block",
    );
  });

  it("renders as inline block when isInline is true", () => {
    render(<Skeleton data-testid="skeleton" isInline />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveClass(
      "animate-pulse",
      "rounded-md",
      "bg-muted",
      "inline-block",
    );
  });

  it("applies custom className", () => {
    render(<Skeleton data-testid="skeleton" className="custom-class" />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveClass("custom-class");
  });

  it("passes through HTML attributes", () => {
    render(
      <Skeleton
        data-testid="skeleton"
        aria-label="Loading"
        style={{ width: "100px" }}
      />,
    );
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveAttribute("aria-label", "Loading");
    expect(skeleton).toHaveStyle({ width: "100px" });
  });

  it("renders children content", () => {
    render(
      <Skeleton data-testid="skeleton">
        <span>Loading content</span>
      </Skeleton>,
    );
    expect(screen.getByText("Loading content")).toBeInTheDocument();
  });
});
