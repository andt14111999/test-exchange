import { render, screen } from "@testing-library/react";
import WalletLayout from "@/app/[locale]/wallet/layout";

describe("WalletLayout", () => {
  it("should render children within the layout", () => {
    // Arrange
    const testContent = "Test Content";

    // Act
    render(
      <WalletLayout>
        <div>{testContent}</div>
      </WalletLayout>,
    );

    // Assert
    expect(screen.getByText(testContent)).toBeInTheDocument();

    // Check if the wrapper div has the correct classes
    const wrapperDiv = screen.getByText(testContent).parentElement;
    expect(wrapperDiv).toHaveClass(
      "min-h-[calc(100vh-theme(spacing.14))]",
      "bg-gradient-to-b",
      "from-background",
      "to-muted",
    );
  });
});
