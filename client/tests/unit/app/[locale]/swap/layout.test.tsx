import { render, screen } from "@testing-library/react";
import SwapLayout from "@/app/[locale]/swap/layout";

// Mock ProtectedLayout component
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

describe("SwapLayout", () => {
  it("renders children within ProtectedLayout", () => {
    const testChild = <div data-testid="test-child">Test Child</div>;

    render(<SwapLayout>{testChild}</SwapLayout>);

    // Check if ProtectedLayout is rendered
    expect(screen.getByTestId("protected-layout")).toBeInTheDocument();

    // Check if child is rendered
    expect(screen.getByTestId("test-child")).toBeInTheDocument();

    // Check if the container div has the correct classes
    const container = screen.getByTestId("protected-layout").firstChild;
    expect(container).toHaveClass(
      "min-h-[calc(100vh-theme(spacing.14))]",
      "bg-gradient-to-b",
      "from-background",
      "to-muted",
    );
  });
});
