import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from "@/components/ui/dialog";

describe("Dialog components", () => {
  describe("Dialog", () => {
    it("should render dialog with trigger and content", () => {
      render(
        <Dialog>
          <DialogTrigger>Open Dialog</DialogTrigger>
          <DialogContent>
            <div>Dialog Content</div>
          </DialogContent>
        </Dialog>,
      );

      const trigger = screen.getByText("Open Dialog");
      expect(trigger).toBeInTheDocument();

      // Click to open dialog
      fireEvent.click(trigger);
      expect(screen.getByText("Dialog Content")).toBeInTheDocument();
    });

    it("should close dialog when clicking close button", () => {
      render(
        <Dialog>
          <DialogTrigger>Open Dialog</DialogTrigger>
          <DialogContent>
            <div>Dialog Content</div>
          </DialogContent>
        </Dialog>,
      );

      // Open dialog
      fireEvent.click(screen.getByText("Open Dialog"));
      expect(screen.getByText("Dialog Content")).toBeInTheDocument();

      // Close dialog
      const closeButton = screen.getByRole("button", { name: /close/i });
      fireEvent.click(closeButton);

      // Content should not be visible
      expect(screen.queryByText("Dialog Content")).not.toBeInTheDocument();
    });
  });

  describe("DialogContent", () => {
    it("should render with custom className", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent className="custom-class">Content</DialogContent>
        </Dialog>,
      );

      const content = screen.getByRole("dialog");
      expect(content).toHaveClass("custom-class");
    });

    it("should render close button by default", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent>Content</DialogContent>
        </Dialog>,
      );

      expect(
        screen.getByRole("button", { name: /close/i }),
      ).toBeInTheDocument();
    });
  });

  describe("DialogHeader", () => {
    it("should render with custom className", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent>
            <DialogHeader className="custom-header" data-testid="dialog-header">
              Header Content
            </DialogHeader>
          </DialogContent>
        </Dialog>,
      );

      const header = screen.getByTestId("dialog-header");
      expect(header).toHaveClass("custom-header");
      expect(header).toHaveClass("flex");
      expect(header).toHaveClass("flex-col");
    });
  });

  describe("DialogFooter", () => {
    it("should render with custom className", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent>
            <DialogFooter className="custom-footer" data-testid="dialog-footer">
              Footer Content
            </DialogFooter>
          </DialogContent>
        </Dialog>,
      );

      const footer = screen.getByTestId("dialog-footer");
      expect(footer).toHaveClass("custom-footer");
      expect(footer).toHaveClass("flex");
      expect(footer).toHaveClass("flex-col-reverse");
    });
  });

  describe("DialogTitle", () => {
    it("should render with custom className", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent>
            <DialogTitle className="custom-title">Dialog Title</DialogTitle>
          </DialogContent>
        </Dialog>,
      );

      const title = screen.getByText("Dialog Title");
      expect(title).toHaveClass("custom-title");
      expect(title).toHaveClass("text-lg");
      expect(title).toHaveClass("font-semibold");
    });
  });

  describe("DialogDescription", () => {
    it("should render with custom className", () => {
      render(
        <Dialog defaultOpen>
          <DialogContent>
            <DialogDescription className="custom-desc">
              Dialog Description
            </DialogDescription>
          </DialogContent>
        </Dialog>,
      );

      const desc = screen.getByText("Dialog Description");
      expect(desc).toHaveClass("custom-desc");
      expect(desc).toHaveClass("text-sm");
      expect(desc).toHaveClass("text-muted-foreground");
    });
  });

  describe("DialogClose", () => {
    it("should close dialog when clicked", () => {
      render(
        <Dialog>
          <DialogTrigger>Open</DialogTrigger>
          <DialogContent>
            <DialogClose>Custom Close</DialogClose>
          </DialogContent>
        </Dialog>,
      );

      // Open dialog
      fireEvent.click(screen.getByText("Open"));
      expect(screen.getByText("Custom Close")).toBeInTheDocument();

      // Close using custom close button
      fireEvent.click(screen.getByText("Custom Close"));
      expect(screen.queryByText("Custom Close")).not.toBeInTheDocument();
    });
  });

  describe("Dialog with all components", () => {
    it("should render a complete dialog with all components", () => {
      render(
        <Dialog>
          <DialogTrigger>Open Complete Dialog</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Complete Dialog Title</DialogTitle>
              <DialogDescription>
                This is a complete dialog example
              </DialogDescription>
            </DialogHeader>
            <div>Main Content</div>
            <DialogFooter>
              <DialogClose>Close Dialog</DialogClose>
            </DialogFooter>
          </DialogContent>
        </Dialog>,
      );

      // Open dialog
      fireEvent.click(screen.getByText("Open Complete Dialog"));

      // Verify all components are rendered
      expect(screen.getByText("Complete Dialog Title")).toBeInTheDocument();
      expect(
        screen.getByText("This is a complete dialog example"),
      ).toBeInTheDocument();
      expect(screen.getByText("Main Content")).toBeInTheDocument();
      expect(screen.getByText("Close Dialog")).toBeInTheDocument();

      // Close dialog
      fireEvent.click(screen.getByText("Close Dialog"));
      expect(
        screen.queryByText("Complete Dialog Title"),
      ).not.toBeInTheDocument();
    });
  });
});
