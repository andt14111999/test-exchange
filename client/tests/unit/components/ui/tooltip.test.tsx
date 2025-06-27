import * as React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  Tooltip,
  TooltipProvider,
  TooltipTrigger,
  TooltipContent,
} from "@/components/ui/tooltip";

describe("Tooltip components", () => {
  describe("TooltipProvider", () => {
    it("should provide tooltip context to children", () => {
      render(
        <TooltipProvider>
          <div>Child content</div>
        </TooltipProvider>,
      );
      expect(screen.getByText("Child content")).toBeInTheDocument();
    });
  });

  describe("Tooltip", () => {
    it("should render with relative positioning", () => {
      render(
        <Tooltip>
          <div>Tooltip content</div>
        </Tooltip>,
      );
      const container = screen.getByText("Tooltip content").parentElement;
      expect(container).toHaveClass("relative", "inline-block");
    });
  });

  describe("TooltipTrigger", () => {
    it("should render as button by default", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
          </Tooltip>
        </TooltipProvider>,
      );
      const trigger = screen.getByRole("button");
      expect(trigger).toHaveTextContent("Trigger");
      expect(trigger).toHaveAttribute("type", "button");
    });

    it("should handle mouse events", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
            <TooltipContent>Content</TooltipContent>
          </Tooltip>
        </TooltipProvider>,
      );

      const trigger = screen.getByRole("button");

      // Initially content should not be visible
      expect(screen.queryByText("Content")).not.toBeInTheDocument();

      // Show on mouse enter
      fireEvent.mouseEnter(trigger);
      expect(screen.getByText("Content")).toBeInTheDocument();

      // Hide on mouse leave
      fireEvent.mouseLeave(trigger);
      expect(screen.queryByText("Content")).not.toBeInTheDocument();
    });

    it("should handle asChild prop with custom element", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>Custom Trigger</span>
            </TooltipTrigger>
          </Tooltip>
        </TooltipProvider>,
      );

      const trigger = screen.getByText("Custom Trigger");
      expect(trigger.tagName.toLowerCase()).toBe("span");
    });

    it("should forward ref to button element", () => {
      const ref = React.createRef<HTMLButtonElement>();
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger ref={ref}>Trigger</TooltipTrigger>
          </Tooltip>
        </TooltipProvider>,
      );
      expect(ref.current).toBeInstanceOf(HTMLButtonElement);
    });
  });

  describe("TooltipContent", () => {
    it("should not render when tooltip is closed", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
            <TooltipContent>Tooltip Content</TooltipContent>
          </Tooltip>
        </TooltipProvider>,
      );
      expect(screen.queryByText("Tooltip Content")).not.toBeInTheDocument();
    });

    it("should render with correct positioning and styling when open", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
            <TooltipContent>Tooltip Content</TooltipContent>
          </Tooltip>
        </TooltipProvider>,
      );

      // Open tooltip
      fireEvent.mouseEnter(screen.getByText("Trigger"));

      expect(screen.getByText("Tooltip Content")).toBeInTheDocument();
      const tooltipContainer = screen
        .getByText("Tooltip Content")
        .closest("div");
      expect(tooltipContainer).toHaveClass(
        "absolute",
        "z-50",
        "px-3",
        "py-1.5",
        "text-xs",
        "rounded",
        "shadow-md",
        "bg-gray-900",
        "text-white",
        "-translate-x-1/2",
        "left-1/2",
        "-top-8",
      );

      // Check arrow element
      const arrow = tooltipContainer?.querySelector(".tooltip-arrow");
      expect(arrow).toHaveClass(
        "absolute",
        "w-2",
        "h-2",
        "bg-gray-900",
        "rotate-45",
        "-bottom-1",
        "left-1/2",
        "-translate-x-1/2",
      );
    });

    it("should handle complex content", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
            <TooltipContent>
              <div>Line 1</div>
              <div>Line 2</div>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>,
      );

      // Open tooltip
      fireEvent.mouseEnter(screen.getByText("Trigger"));

      expect(screen.getByText("Line 1")).toBeInTheDocument();
      expect(screen.getByText("Line 2")).toBeInTheDocument();
    });
  });

  describe("Integration", () => {
    it("should handle multiple tooltips", () => {
      render(
        <TooltipProvider>
          <div>
            <Tooltip>
              <TooltipTrigger>First</TooltipTrigger>
              <TooltipContent>First Content</TooltipContent>
            </Tooltip>
            <Tooltip>
              <TooltipTrigger>Second</TooltipTrigger>
              <TooltipContent>Second Content</TooltipContent>
            </Tooltip>
          </div>
        </TooltipProvider>,
      );

      // Initially no tooltips visible
      expect(screen.queryByText("First Content")).not.toBeInTheDocument();
      expect(screen.queryByText("Second Content")).not.toBeInTheDocument();

      // Show first tooltip
      fireEvent.mouseEnter(screen.getByText("First"));
      expect(screen.getByText("First Content")).toBeInTheDocument();

      // Show second tooltip
      fireEvent.mouseEnter(screen.getByText("Second"));
      expect(screen.getByText("Second Content")).toBeInTheDocument();

      // Hide first tooltip
      fireEvent.mouseLeave(screen.getByText("First"));
      expect(screen.queryByText("First Content")).not.toBeInTheDocument();
    });

    it("should maintain tooltip state across mouse events", () => {
      render(
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>Trigger</TooltipTrigger>
            <TooltipContent>Content</TooltipContent>
          </Tooltip>
        </TooltipProvider>,
      );

      const trigger = screen.getByText("Trigger");

      // Show tooltip
      fireEvent.mouseEnter(trigger);
      expect(screen.getByText("Content")).toBeInTheDocument();

      // Hide tooltip
      fireEvent.mouseLeave(trigger);
      expect(screen.queryByText("Content")).not.toBeInTheDocument();

      // Show tooltip again
      fireEvent.mouseEnter(trigger);
      expect(screen.getByText("Content")).toBeInTheDocument();
    });
  });
});
