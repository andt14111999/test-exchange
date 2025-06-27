import React from "react";
import { render } from "@testing-library/react";
import DepositLayout from "@/app/[locale]/deposit/layout";

// Mock ProtectedLayout component
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

describe("DepositLayout", () => {
  const mockChildren = <div data-testid="test-children">Test Content</div>;

  it("should render the layout with children", () => {
    const { getByTestId } = render(
      <DepositLayout>{mockChildren}</DepositLayout>,
    );

    // Check if ProtectedLayout is rendered
    expect(getByTestId("protected-layout")).toBeInTheDocument();

    // Check if children are rendered
    expect(getByTestId("test-children")).toBeInTheDocument();
  });

  it("should have correct styling classes", () => {
    const { container } = render(<DepositLayout>{mockChildren}</DepositLayout>);

    const mainDiv = container.firstChild?.firstChild as HTMLElement;
    expect(mainDiv).toHaveClass(
      "min-h-[calc(100vh-theme(spacing.14))]",
      "bg-gradient-to-b",
      "from-background",
      "to-muted",
      "mx-auto",
      "max-w-screen-lg",
    );
  });

  it("should match snapshot", () => {
    const { container } = render(<DepositLayout>{mockChildren}</DepositLayout>);
    expect(container).toMatchSnapshot();
  });
});
