import * as React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
  DropdownMenuRadioItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuGroup,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuRadioGroup,
} from "@/components/ui/dropdown-menu";

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

describe("DropdownMenu Components", () => {
  const user = userEvent.setup();

  describe("Basic Dropdown", () => {
    it("renders a basic dropdown menu", async () => {
      render(
        <DropdownMenu>
          <DropdownMenuTrigger data-testid="trigger">Open</DropdownMenuTrigger>
          <DropdownMenuContent data-testid="content">
            <DropdownMenuItem data-testid="item">Item</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const trigger = screen.getByTestId("trigger");
      expect(trigger).toBeInTheDocument();

      await user.click(trigger);
      expect(screen.getByTestId("content")).toBeInTheDocument();
      expect(screen.getByTestId("item")).toBeInTheDocument();
    });
  });

  describe("DropdownMenuItem", () => {
    it("renders with default and custom styles", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem
              data-testid="menu-item"
              className="custom-class"
              inset
            >
              Test Item
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const menuItem = screen.getByTestId("menu-item");
      expect(menuItem).toHaveClass("custom-class");
      expect(menuItem).toHaveClass("pl-8"); // inset class
    });

    it("handles disabled state", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem data-testid="menu-item" disabled>
              Disabled Item
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const menuItem = screen.getByTestId("menu-item");
      expect(menuItem).toHaveClass("data-[disabled]:pointer-events-none");
      expect(menuItem).toHaveClass("data-[disabled]:opacity-50");
    });
  });

  describe("DropdownMenuCheckboxItem", () => {
    it("renders checkbox item with checked state", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuCheckboxItem data-testid="checkbox-item" checked>
              Checkbox Item
            </DropdownMenuCheckboxItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const checkboxItem = screen.getByTestId("checkbox-item");
      expect(checkboxItem).toBeInTheDocument();
      expect(checkboxItem).toHaveAttribute("data-state", "checked");
    });

    it("applies custom className", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuCheckboxItem
              data-testid="checkbox-item"
              className="custom-class"
            >
              Checkbox Item
            </DropdownMenuCheckboxItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const checkboxItem = screen.getByTestId("checkbox-item");
      expect(checkboxItem).toHaveClass("custom-class");
    });
  });

  describe("DropdownMenuRadioGroup", () => {
    it("renders radio group with items", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuRadioGroup value="option1">
              <DropdownMenuRadioItem data-testid="radio-1" value="option1">
                Option 1
              </DropdownMenuRadioItem>
              <DropdownMenuRadioItem data-testid="radio-2" value="option2">
                Option 2
              </DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const radio1 = screen.getByTestId("radio-1");
      const radio2 = screen.getByTestId("radio-2");
      expect(radio1).toHaveAttribute("data-state", "checked");
      expect(radio2).toHaveAttribute("data-state", "unchecked");
    });
  });

  describe("DropdownMenuLabel", () => {
    it("renders with default and custom styles", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuLabel
              data-testid="label"
              className="custom-class"
              inset
            >
              Label
            </DropdownMenuLabel>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const label = screen.getByTestId("label");
      expect(label).toHaveClass("custom-class");
      expect(label).toHaveClass("pl-8"); // inset class
    });
  });

  describe("DropdownMenuSeparator", () => {
    it("renders with default and custom styles", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuSeparator
              data-testid="separator"
              className="custom-class"
            />
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const separator = screen.getByTestId("separator");
      expect(separator).toHaveClass("custom-class");
      expect(separator).toHaveClass("h-px");
      expect(separator).toHaveClass("bg-muted");
    });
  });

  describe("DropdownMenuShortcut", () => {
    it("renders with default and custom styles", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem>
              <DropdownMenuShortcut
                data-testid="shortcut"
                className="custom-class"
              >
                ⌘K
              </DropdownMenuShortcut>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const shortcut = screen.getByTestId("shortcut");
      expect(shortcut).toHaveClass("custom-class");
      expect(shortcut).toHaveClass("ml-auto");
      expect(shortcut).toHaveClass("text-xs");
    });
  });

  describe("Nested Dropdown", () => {
    it("renders nested dropdown with sub-trigger and sub-content", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger data-testid="main-trigger">
            Open
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuSub>
              <DropdownMenuSubTrigger data-testid="sub-trigger">
                More Options
              </DropdownMenuSubTrigger>
              <DropdownMenuSubContent data-testid="sub-content">
                <DropdownMenuItem>Sub Item</DropdownMenuItem>
              </DropdownMenuSubContent>
            </DropdownMenuSub>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      const subTrigger = screen.getByTestId("sub-trigger");
      expect(subTrigger).toBeInTheDocument();

      // Hover and wait for animation
      await user.hover(subTrigger);
      await sleep(300); // Wait for animation/transition

      const subContent = screen.getByTestId("sub-content");
      expect(subContent).toBeInTheDocument();
    });
  });

  describe("Complex Integration", () => {
    it("renders a complete dropdown with all components", async () => {
      render(
        <DropdownMenu defaultOpen>
          <DropdownMenuTrigger data-testid="trigger">Menu</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuLabel>My Account</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem>Profile</DropdownMenuItem>
              <DropdownMenuItem>Settings</DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuCheckboxItem checked>
              Show Status
            </DropdownMenuCheckboxItem>
            <DropdownMenuSeparator />
            <DropdownMenuRadioGroup value="bento">
              <DropdownMenuRadioItem value="bento">Bento</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="stack">Stack</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              Keyboard Shortcuts
              <DropdownMenuShortcut>⌘K</DropdownMenuShortcut>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>,
      );

      expect(screen.getByText("My Account")).toBeInTheDocument();
      expect(screen.getByText("Profile")).toBeInTheDocument();
      expect(screen.getByText("Settings")).toBeInTheDocument();
      expect(screen.getByText("Show Status")).toBeInTheDocument();
      expect(screen.getByText("Bento")).toBeInTheDocument();
      expect(screen.getByText("Stack")).toBeInTheDocument();
      expect(screen.getByText("⌘K")).toBeInTheDocument();
    });
  });
});
