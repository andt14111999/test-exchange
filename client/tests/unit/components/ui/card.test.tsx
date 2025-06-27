import * as React from "react";
import { render, screen } from "@testing-library/react";
import {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";

describe("Card Components", () => {
  describe("Card", () => {
    it("renders with default styles", () => {
      render(<Card data-testid="card">Card Content</Card>);
      const card = screen.getByTestId("card");
      expect(card).toHaveClass(
        "rounded-lg",
        "border",
        "bg-card",
        "text-card-foreground",
        "shadow-sm",
      );
    });

    it("applies custom className", () => {
      render(
        <Card data-testid="card" className="custom-class">
          Card Content
        </Card>,
      );
      const card = screen.getByTestId("card");
      expect(card).toHaveClass("custom-class");
    });
  });

  describe("CardHeader", () => {
    it("renders with default styles", () => {
      render(<CardHeader data-testid="header">Header Content</CardHeader>);
      const header = screen.getByTestId("header");
      expect(header).toHaveClass("flex", "flex-col", "space-y-1.5", "p-6");
    });

    it("applies custom className", () => {
      render(
        <CardHeader data-testid="header" className="custom-class">
          Header Content
        </CardHeader>,
      );
      const header = screen.getByTestId("header");
      expect(header).toHaveClass("custom-class");
    });
  });

  describe("CardTitle", () => {
    it("renders with default styles", () => {
      render(<CardTitle data-testid="title">Title Content</CardTitle>);
      const title = screen.getByTestId("title");
      expect(title).toHaveClass(
        "text-2xl",
        "font-semibold",
        "leading-none",
        "tracking-tight",
      );
    });

    it("applies custom className", () => {
      render(
        <CardTitle data-testid="title" className="custom-class">
          Title Content
        </CardTitle>,
      );
      const title = screen.getByTestId("title");
      expect(title).toHaveClass("custom-class");
    });
  });

  describe("CardDescription", () => {
    it("renders with default styles", () => {
      render(
        <CardDescription data-testid="description">
          Description Content
        </CardDescription>,
      );
      const description = screen.getByTestId("description");
      expect(description).toHaveClass("text-sm", "text-muted-foreground");
    });

    it("applies custom className", () => {
      render(
        <CardDescription data-testid="description" className="custom-class">
          Description Content
        </CardDescription>,
      );
      const description = screen.getByTestId("description");
      expect(description).toHaveClass("custom-class");
    });
  });

  describe("CardContent", () => {
    it("renders with default styles", () => {
      render(<CardContent data-testid="content">Content</CardContent>);
      const content = screen.getByTestId("content");
      expect(content).toHaveClass("p-6", "pt-0");
    });

    it("applies custom className", () => {
      render(
        <CardContent data-testid="content" className="custom-class">
          Content
        </CardContent>,
      );
      const content = screen.getByTestId("content");
      expect(content).toHaveClass("custom-class");
    });
  });

  describe("CardFooter", () => {
    it("renders with default styles", () => {
      render(<CardFooter data-testid="footer">Footer Content</CardFooter>);
      const footer = screen.getByTestId("footer");
      expect(footer).toHaveClass("flex", "items-center", "p-6", "pt-0");
    });

    it("applies custom className", () => {
      render(
        <CardFooter data-testid="footer" className="custom-class">
          Footer Content
        </CardFooter>,
      );
      const footer = screen.getByTestId("footer");
      expect(footer).toHaveClass("custom-class");
    });
  });

  describe("Integration", () => {
    it("renders a complete card with all components", () => {
      render(
        <Card data-testid="card">
          <CardHeader>
            <CardTitle>Card Title</CardTitle>
            <CardDescription>Card Description</CardDescription>
          </CardHeader>
          <CardContent>Card Content</CardContent>
          <CardFooter>Card Footer</CardFooter>
        </Card>,
      );

      expect(screen.getByText("Card Title")).toBeInTheDocument();
      expect(screen.getByText("Card Description")).toBeInTheDocument();
      expect(screen.getByText("Card Content")).toBeInTheDocument();
      expect(screen.getByText("Card Footer")).toBeInTheDocument();
    });
  });
});
