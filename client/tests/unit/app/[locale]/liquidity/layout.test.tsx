import React from "react";
import { render } from "@testing-library/react";
import LiquidityLayout from "@/app/[locale]/liquidity/layout";

// Mock ProtectedLayout component
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

// Mock AmmPoolProvider component
jest.mock("@/providers/amm-pool-provider", () => ({
  AmmPoolProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="amm-pool-provider">{children}</div>
  ),
}));

describe("LiquidityLayout", () => {
  const mockChildren = <div data-testid="test-children">Test Content</div>;

  it("should render the layout with children", () => {
    const { getByTestId } = render(
      <LiquidityLayout>{mockChildren}</LiquidityLayout>,
    );

    // Check if ProtectedLayout is rendered
    expect(getByTestId("protected-layout")).toBeInTheDocument();

    // Check if AmmPoolProvider is rendered
    expect(getByTestId("amm-pool-provider")).toBeInTheDocument();

    // Check if children are rendered
    expect(getByTestId("test-children")).toBeInTheDocument();
  });

  it("should have correct styling classes", () => {
    const { container } = render(
      <LiquidityLayout>{mockChildren}</LiquidityLayout>,
    );

    const mainDiv = container.firstChild?.firstChild as HTMLElement;
    expect(mainDiv).toHaveClass(
      "min-h-[calc(100vh-theme(spacing.14))]",
      "bg-gradient-to-b",
      "from-background",
      "to-muted",
    );
  });

  it("should match snapshot", () => {
    const { container } = render(
      <LiquidityLayout>{mockChildren}</LiquidityLayout>,
    );
    expect(container).toMatchSnapshot();
  });
});
