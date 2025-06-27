import { render, screen, fireEvent } from "@testing-library/react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import "@testing-library/jest-dom";

describe("Tabs Components", () => {
  const renderTabs = () => {
    return render(
      <Tabs defaultValue="tab1">
        <TabsList>
          <TabsTrigger value="tab1">Tab 1</TabsTrigger>
          <TabsTrigger value="tab2">Tab 2</TabsTrigger>
        </TabsList>
        <TabsContent value="tab1">Content 1</TabsContent>
        <TabsContent value="tab2">Content 2</TabsContent>
      </Tabs>,
    );
  };

  describe("Tabs Root", () => {
    it("renders correctly with children", () => {
      renderTabs();
      expect(screen.getByRole("tablist")).toBeInTheDocument();
      expect(screen.getAllByRole("tab")).toHaveLength(2);
      expect(screen.getAllByRole("tabpanel")).toHaveLength(1); // Only active tab panel is rendered
    });
  });

  describe("TabsList", () => {
    it("renders with default styles", () => {
      renderTabs();
      const tabList = screen.getByRole("tablist");
      expect(tabList).toHaveClass(
        "inline-flex",
        "h-9",
        "items-center",
        "justify-center",
        "rounded-lg",
        "bg-muted",
        "p-1",
        "text-muted-foreground",
      );
    });

    it("applies custom className correctly", () => {
      render(
        <Tabs>
          <TabsList className="custom-class">
            <TabsTrigger value="tab1">Tab 1</TabsTrigger>
          </TabsList>
        </Tabs>,
      );
      const tabList = screen.getByRole("tablist");
      expect(tabList).toHaveClass("custom-class");
    });
  });

  describe("TabsTrigger", () => {
    it("renders with default styles", () => {
      renderTabs();
      const triggers = screen.getAllByRole("tab");
      triggers.forEach((trigger) => {
        expect(trigger).toHaveClass(
          "inline-flex",
          "items-center",
          "justify-center",
          "whitespace-nowrap",
          "rounded-md",
          "px-3",
          "py-1",
          "text-sm",
          "font-medium",
        );
      });
    });

    it("applies active styles when selected", () => {
      renderTabs();
      const activeTab = screen.getByRole("tab", { selected: true });
      expect(activeTab).toHaveAttribute("data-state", "active");
      expect(activeTab).toHaveClass(
        "data-[state=active]:bg-background",
        "data-[state=active]:text-foreground",
        "data-[state=active]:shadow",
      );
    });

    it("handles click events correctly", async () => {
      render(
        <Tabs value="tab1" onValueChange={() => {}}>
          <TabsList>
            <TabsTrigger value="tab1">Tab 1</TabsTrigger>
            <TabsTrigger value="tab2">Tab 2</TabsTrigger>
          </TabsList>
          <TabsContent value="tab1">Content 1</TabsContent>
          <TabsContent value="tab2">Content 2</TabsContent>
        </Tabs>,
      );

      const secondTab = screen.getByRole("tab", { name: "Tab 2" });
      fireEvent.click(secondTab);

      expect(secondTab).toHaveAttribute("data-state", "inactive");
      expect(screen.queryByText("Content 2")).not.toBeInTheDocument();
    });

    it("applies custom className correctly", () => {
      render(
        <Tabs>
          <TabsList>
            <TabsTrigger value="tab1" className="custom-trigger">
              Tab 1
            </TabsTrigger>
          </TabsList>
        </Tabs>,
      );
      const trigger = screen.getByRole("tab");
      expect(trigger).toHaveClass("custom-trigger");
    });

    it("handles disabled state correctly", () => {
      render(
        <Tabs>
          <TabsList>
            <TabsTrigger value="tab1" disabled>
              Tab 1
            </TabsTrigger>
          </TabsList>
        </Tabs>,
      );
      const trigger = screen.getByRole("tab");
      expect(trigger).toBeDisabled();
      expect(trigger).toHaveClass(
        "disabled:pointer-events-none",
        "disabled:opacity-50",
      );
    });
  });

  describe("TabsContent", () => {
    it("renders content when tab is active", () => {
      renderTabs();
      expect(screen.getByText("Content 1")).toBeInTheDocument();
      expect(screen.queryByText("Content 2")).not.toBeInTheDocument();
    });

    it("applies default styles", () => {
      renderTabs();
      const content = screen.getByRole("tabpanel");
      expect(content).toHaveClass(
        "mt-2",
        "ring-offset-background",
        "focus-visible:outline-none",
        "focus-visible:ring-2",
        "focus-visible:ring-ring",
        "focus-visible:ring-offset-2",
      );
    });

    it("applies custom className correctly", () => {
      render(
        <Tabs defaultValue="tab1">
          <TabsList>
            <TabsTrigger value="tab1">Tab 1</TabsTrigger>
          </TabsList>
          <TabsContent value="tab1" className="custom-content">
            Content 1
          </TabsContent>
        </Tabs>,
      );
      const content = screen.getByRole("tabpanel");
      expect(content).toHaveClass("custom-content");
    });

    it("handles keyboard navigation correctly", async () => {
      render(
        <Tabs defaultValue="tab1" onValueChange={() => {}}>
          <TabsList>
            <TabsTrigger value="tab1">Tab 1</TabsTrigger>
            <TabsTrigger value="tab2">Tab 2</TabsTrigger>
          </TabsList>
          <TabsContent value="tab1">Content 1</TabsContent>
          <TabsContent value="tab2">Content 2</TabsContent>
        </Tabs>,
      );

      const tabList = screen.getByRole("tablist");
      const secondTab = screen.getByRole("tab", { name: "Tab 2" });

      // Focus the tablist
      fireEvent.focus(tabList);

      // Press arrow right to move to second tab
      fireEvent.keyDown(tabList, { key: "ArrowRight" });

      expect(secondTab).toHaveAttribute("data-state", "inactive");
    });
  });
});
