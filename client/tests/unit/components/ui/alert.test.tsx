import React from "react";
import { render, screen } from "@testing-library/react";
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";

describe("Alert Component", () => {
  describe("Alert", () => {
    it("renders with default variant", () => {
      render(<Alert>Test Alert</Alert>);
      const alert = screen.getByRole("alert");
      expect(alert).toHaveClass("bg-background", "text-foreground");
    });

    it("renders with destructive variant", () => {
      render(<Alert variant="destructive">Test Alert</Alert>);
      const alert = screen.getByRole("alert");
      expect(alert).toHaveClass("border-destructive/50", "text-destructive");
    });

    it("applies custom className", () => {
      render(<Alert className="custom-class">Test Alert</Alert>);
      const alert = screen.getByRole("alert");
      expect(alert).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLDivElement>();
      render(<Alert ref={ref}>Test Alert</Alert>);
      expect(ref.current).toBeInstanceOf(HTMLDivElement);
    });

    it("spreads additional props", () => {
      render(<Alert data-testid="test-alert">Test Alert</Alert>);
      expect(screen.getByTestId("test-alert")).toBeInTheDocument();
    });
  });

  describe("AlertTitle", () => {
    it("renders with correct default styles", () => {
      render(<AlertTitle>Test Title</AlertTitle>);
      const title = screen.getByText("Test Title");
      expect(title).toHaveClass("text-sm", "font-medium");
    });

    it("applies custom className", () => {
      render(<AlertTitle className="custom-class">Test Title</AlertTitle>);
      const title = screen.getByText("Test Title");
      expect(title).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLParagraphElement>();
      render(<AlertTitle ref={ref}>Test Title</AlertTitle>);
      expect(ref.current).toBeInstanceOf(HTMLHeadingElement);
    });

    it("spreads additional props", () => {
      render(<AlertTitle data-testid="test-title">Test Title</AlertTitle>);
      expect(screen.getByTestId("test-title")).toBeInTheDocument();
    });
  });

  describe("AlertDescription", () => {
    it("renders with correct default styles", () => {
      render(<AlertDescription>Test Description</AlertDescription>);
      const description = screen.getByText("Test Description");
      expect(description).toHaveClass("text-sm");
    });

    it("applies custom className", () => {
      render(
        <AlertDescription className="custom-class">
          Test Description
        </AlertDescription>,
      );
      const description = screen.getByText("Test Description");
      expect(description).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLParagraphElement>();
      render(<AlertDescription ref={ref}>Test Description</AlertDescription>);
      expect(ref.current).toBeInstanceOf(HTMLDivElement);
    });

    it("spreads additional props", () => {
      render(
        <AlertDescription data-testid="test-description">
          Test Description
        </AlertDescription>,
      );
      expect(screen.getByTestId("test-description")).toBeInTheDocument();
    });
  });

  describe("Alert Component Integration", () => {
    it("renders with title and description", () => {
      render(
        <Alert>
          <AlertTitle>Alert Title</AlertTitle>
          <AlertDescription>Alert Description</AlertDescription>
        </Alert>,
      );

      expect(screen.getByText("Alert Title")).toBeInTheDocument();
      expect(screen.getByText("Alert Description")).toBeInTheDocument();
    });

    it("renders destructive variant with title and description", () => {
      render(
        <Alert variant="destructive">
          <AlertTitle>Error Title</AlertTitle>
          <AlertDescription>Error Description</AlertDescription>
        </Alert>,
      );

      const alert = screen.getByRole("alert");
      expect(alert).toHaveClass("border-destructive/50", "text-destructive");
      expect(screen.getByText("Error Title")).toBeInTheDocument();
      expect(screen.getByText("Error Description")).toBeInTheDocument();
    });
  });
});
