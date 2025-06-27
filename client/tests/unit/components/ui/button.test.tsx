import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Button, buttonVariants } from "@/components/ui/button";

describe("Button component", () => {
  it("should render correctly", () => {
    render(<Button>Click me</Button>);
    const button = screen.getByRole("button", { name: /click me/i });
    expect(button).toBeInTheDocument();
  });

  describe("variants", () => {
    it("should apply default variant class", () => {
      render(<Button>Default Button</Button>);
      const button = screen.getByRole("button", { name: /default button/i });
      expect(button).toHaveClass("bg-primary");
      expect(button).toHaveClass("text-primary-foreground");
    });

    it("should apply destructive variant class", () => {
      render(<Button variant="destructive">Destructive Button</Button>);
      const button = screen.getByRole("button", {
        name: /destructive button/i,
      });
      expect(button).toHaveClass("bg-destructive");
      expect(button).toHaveClass("text-destructive-foreground");
    });

    it("should apply outline variant class", () => {
      render(<Button variant="outline">Outline Button</Button>);
      const button = screen.getByRole("button", { name: /outline button/i });
      expect(button).toHaveClass("border-input");
      expect(button).toHaveClass("bg-background");
    });

    it("should apply secondary variant class", () => {
      render(<Button variant="secondary">Secondary Button</Button>);
      const button = screen.getByRole("button", { name: /secondary button/i });
      expect(button).toHaveClass("bg-secondary");
      expect(button).toHaveClass("text-secondary-foreground");
    });

    it("should apply ghost variant class", () => {
      render(<Button variant="ghost">Ghost Button</Button>);
      const button = screen.getByRole("button", { name: /ghost button/i });
      expect(button).toHaveClass("hover:bg-accent");
      expect(button).toHaveClass("hover:text-accent-foreground");
    });

    it("should apply link variant class", () => {
      render(<Button variant="link">Link Button</Button>);
      const button = screen.getByRole("button", { name: /link button/i });
      expect(button).toHaveClass("text-primary");
      expect(button).toHaveClass("hover:underline");
    });
  });

  describe("sizes", () => {
    it("should apply default size class", () => {
      render(<Button>Default Size</Button>);
      const button = screen.getByRole("button", { name: /default size/i });
      expect(button).toHaveClass("h-10");
      expect(button).toHaveClass("px-4");
      expect(button).toHaveClass("py-2");
    });

    it("should apply sm size class", () => {
      render(<Button size="sm">Small Button</Button>);
      const button = screen.getByRole("button", { name: /small button/i });
      expect(button).toHaveClass("h-9");
      expect(button).toHaveClass("px-3");
    });

    it("should apply lg size class", () => {
      render(<Button size="lg">Large Button</Button>);
      const button = screen.getByRole("button", { name: /large button/i });
      expect(button).toHaveClass("h-11");
      expect(button).toHaveClass("px-8");
    });

    it("should apply icon size class", () => {
      render(<Button size="icon">Icon</Button>);
      const button = screen.getByRole("button", { name: /icon/i });
      expect(button).toHaveClass("h-10");
      expect(button).toHaveClass("w-10");
    });
  });

  describe("asChild prop", () => {
    it("should render as a child component when asChild is true", () => {
      render(
        <Button asChild>
          <a href="https://example.com">Link Button</a>
        </Button>,
      );
      const link = screen.getByRole("link", { name: /link button/i });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute("href", "https://example.com");
      expect(link).toHaveClass("bg-primary");
    });

    it("should render as button when asChild is false", () => {
      render(<Button asChild={false}>Normal Button</Button>);
      const button = screen.getByRole("button", { name: /normal button/i });
      expect(button.tagName.toLowerCase()).toBe("button");
    });
  });

  describe("event handling", () => {
    it("should handle onClick event", () => {
      const handleClick = jest.fn();
      render(<Button onClick={handleClick}>Click me</Button>);
      const button = screen.getByRole("button", { name: /click me/i });

      fireEvent.click(button);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it("should not trigger onClick when disabled", () => {
      const handleClick = jest.fn();
      render(
        <Button disabled onClick={handleClick}>
          Disabled Button
        </Button>,
      );
      const button = screen.getByRole("button", { name: /disabled button/i });

      fireEvent.click(button);
      expect(handleClick).not.toHaveBeenCalled();
      expect(button).toBeDisabled();
      expect(button).toHaveClass("disabled:pointer-events-none");
      expect(button).toHaveClass("disabled:opacity-50");
    });
  });

  describe("className prop", () => {
    it("should merge custom className with default classes", () => {
      render(<Button className="custom-class">Custom Button</Button>);
      const button = screen.getByRole("button", { name: /custom button/i });
      expect(button).toHaveClass("custom-class");
      expect(button).toHaveClass("bg-primary"); // Default variant class should still be present
    });
  });

  describe("buttonVariants", () => {
    it("should return correct classes for default variants", () => {
      const classes = buttonVariants({});
      expect(classes).toContain("bg-primary");
      expect(classes).toContain("h-10");
    });

    it("should return correct classes for custom variants", () => {
      const classes = buttonVariants({ variant: "destructive", size: "lg" });
      expect(classes).toContain("bg-destructive");
      expect(classes).toContain("h-11");
    });
  });
});
