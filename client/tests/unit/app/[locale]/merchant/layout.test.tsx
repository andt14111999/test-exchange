import { render, screen } from "@testing-library/react";
import MerchantLayout from "@/app/[locale]/merchant/layout";

// Mock the ProtectedLayout component
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

describe("MerchantLayout", () => {
  const mockChildren = <div data-testid="mock-children">Test Content</div>;

  it("renders the ProtectedLayout component", () => {
    render(<MerchantLayout>{mockChildren}</MerchantLayout>);
    expect(screen.getByTestId("protected-layout")).toBeInTheDocument();
  });

  it("renders children within the layout", () => {
    render(<MerchantLayout>{mockChildren}</MerchantLayout>);
    expect(screen.getByTestId("mock-children")).toBeInTheDocument();
    expect(screen.getByText("Test Content")).toBeInTheDocument();
  });

  it("applies correct layout styles", () => {
    render(<MerchantLayout>{mockChildren}</MerchantLayout>);
    const container = screen.getByTestId("protected-layout").firstChild;

    // Check if the container has the correct classes
    expect(container).toHaveClass("min-h-[calc(100vh-theme(spacing.14))]");
    expect(container).toHaveClass("bg-gradient-to-b");
    expect(container).toHaveClass("from-background");
    expect(container).toHaveClass("to-muted");
  });

  it("renders with different children content", () => {
    const differentChildren = (
      <div data-testid="different-children">Different Content</div>
    );
    render(<MerchantLayout>{differentChildren}</MerchantLayout>);

    expect(screen.getByTestId("different-children")).toBeInTheDocument();
    expect(screen.getByText("Different Content")).toBeInTheDocument();
  });
});
