import { render, screen, fireEvent } from "@testing-library/react";
import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
  NavigationMenuTrigger,
  NavigationMenuContent,
  NavigationMenuLink,
  NavigationMenuIndicator,
  navigationMenuTriggerStyle,
} from "@/components/layout/navigation-menu";

describe("Navigation Menu Components", () => {
  describe("NavigationMenu", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu className="custom-class">
          <div>Content</div>
        </NavigationMenu>,
      );

      const root = screen.getByRole("navigation");
      expect(root).toHaveClass("custom-class");
    });
  });

  describe("NavigationMenuList", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu>
          <NavigationMenuList className="custom-list">
            <div>Content</div>
          </NavigationMenuList>
        </NavigationMenu>,
      );

      const list = screen.getByRole("list");
      expect(list).toHaveClass("custom-list");
    });
  });

  describe("NavigationMenuItem", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu>
          <NavigationMenuList>
            <NavigationMenuItem className="custom-item">
              <div>Content</div>
            </NavigationMenuItem>
          </NavigationMenuList>
        </NavigationMenu>,
      );

      const item = screen.getByRole("listitem");
      expect(item).toHaveClass("custom-item");
    });
  });

  describe("NavigationMenuTrigger", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu>
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuTrigger className="custom-trigger">
                Trigger
              </NavigationMenuTrigger>
            </NavigationMenuItem>
          </NavigationMenuList>
        </NavigationMenu>,
      );

      const trigger = screen.getByRole("button");
      expect(trigger).toHaveClass("custom-trigger");
    });
  });

  describe("NavigationMenuContent", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu>
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuTrigger>Trigger</NavigationMenuTrigger>
              <NavigationMenuContent
                className="custom-content"
                data-testid="content"
              >
                Content
              </NavigationMenuContent>
            </NavigationMenuItem>
          </NavigationMenuList>
        </NavigationMenu>,
      );

      const trigger = screen.getByRole("button");
      fireEvent.click(trigger);

      const content = screen.getByTestId("content");
      expect(content).toHaveClass("custom-content");
    });
  });

  describe("NavigationMenuLink", () => {
    it("renders with custom className", () => {
      render(
        <NavigationMenu>
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuLink className="custom-link" href="#">
                Link
              </NavigationMenuLink>
            </NavigationMenuItem>
          </NavigationMenuList>
        </NavigationMenu>,
      );

      const link = screen.getByRole("link");
      expect(link).toHaveClass("custom-link");
    });
  });

  describe("NavigationMenuIndicator", () => {
    it("integrates with NavigationMenu", () => {
      const { container } = render(
        <NavigationMenu>
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuTrigger>Trigger</NavigationMenuTrigger>
              <NavigationMenuContent>Content</NavigationMenuContent>
            </NavigationMenuItem>
            <NavigationMenuIndicator className="custom-indicator" />
          </NavigationMenuList>
        </NavigationMenu>,
      );

      // Verify that the navigation menu system is properly set up
      expect(
        container.querySelector('nav[aria-label="Main"]'),
      ).toBeInTheDocument();
      expect(
        container.querySelector('ul[data-orientation="horizontal"]'),
      ).toBeInTheDocument();
      expect(container.querySelector("li")).toBeInTheDocument();
      expect(container.querySelector("button[data-state]")).toBeInTheDocument();
    });
  });

  describe("navigationMenuTriggerStyle", () => {
    it("applies correct styles", () => {
      const styles = navigationMenuTriggerStyle();
      expect(styles).toContain("group inline-flex");
      expect(styles).toContain("h-9 w-max items-center");
    });
  });
});
