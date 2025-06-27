import { render, screen } from "@testing-library/react";
import WithdrawLayout from "@/app/[locale]/withdraw/layout";

describe("WithdrawLayout", () => {
  it("should render children within the layout", () => {
    // Arrange
    const testContent = "Test Content";

    // Act
    render(
      <WithdrawLayout>
        <div>{testContent}</div>
      </WithdrawLayout>,
    );

    // Assert
    expect(screen.getByText(testContent)).toBeInTheDocument();
  });

  it("should have correct styling classes", () => {
    // Arrange
    const testContent = "Test Content";

    // Act
    const { container } = render(
      <WithdrawLayout>
        <div>{testContent}</div>
      </WithdrawLayout>,
    );

    // Assert
    const layoutDiv = container.firstChild as HTMLElement;
    expect(layoutDiv).toHaveClass(
      "min-h-[calc(100vh-theme(spacing.14))]",
      "bg-gradient-to-b",
      "from-background",
      "to-muted",
      "mx-auto",
      "max-w-screen-lg",
    );
  });
});
