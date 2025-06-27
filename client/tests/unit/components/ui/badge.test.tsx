import React from "react";
import { render, screen } from "@testing-library/react";
import { Badge, badgeVariants } from "@/components/ui/badge";

describe("Badge component", () => {
  it("renders correctly with default variant", () => {
    render(<Badge>Default Badge</Badge>);
    const badge = screen.getByText("Default Badge");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-primary");
    expect(badge).toHaveClass("text-primary-foreground");
  });

  it("renders with secondary variant", () => {
    render(<Badge variant="secondary">Secondary Badge</Badge>);
    const badge = screen.getByText("Secondary Badge");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-secondary");
    expect(badge).toHaveClass("text-secondary-foreground");
  });

  it("renders with destructive variant", () => {
    render(<Badge variant="destructive">Destructive Badge</Badge>);
    const badge = screen.getByText("Destructive Badge");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-destructive");
    expect(badge).toHaveClass("text-destructive-foreground");
  });

  it("renders with outline variant", () => {
    render(<Badge variant="outline">Outline Badge</Badge>);
    const badge = screen.getByText("Outline Badge");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("text-foreground");
  });

  it("applies custom className", () => {
    const customClass = "custom-class";
    render(<Badge className={customClass}>Custom Badge</Badge>);
    const badge = screen.getByText("Custom Badge");
    expect(badge).toHaveClass(customClass);
  });

  it("forwards additional HTML attributes", () => {
    render(
      <Badge data-testid="test-badge" title="badge title">
        Test Badge
      </Badge>,
    );
    const badge = screen.getByTestId("test-badge");
    expect(badge).toHaveAttribute("title", "badge title");
  });

  it("badgeVariants function returns correct classes", () => {
    const variants = [
      "default",
      "secondary",
      "destructive",
      "outline",
    ] as const;
    variants.forEach((variant) => {
      const classes = badgeVariants({ variant });
      expect(classes).toContain("inline-flex items-center rounded-full border");
      expect(classes).toContain("px-2.5 py-0.5 text-xs font-semibold");
      expect(classes).toContain(
        "transition-colors focus:outline-none focus:ring-2",
      );
      expect(classes).toContain("focus:ring-ring focus:ring-offset-2");
    });
  });
});
