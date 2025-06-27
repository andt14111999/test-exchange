import { render, screen, fireEvent } from "@testing-library/react";
import { useTranslations } from "next-intl";
import StatusTabs, {
  TabStatus,
} from "@/app/[locale]/liquidity/positions/components/StatusTabs";
import React from "react";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Context to simulate Tabs onValueChange propagation
const TabsContext = React.createContext<
  { onValueChange: (value: string) => void } | undefined
>(undefined);

interface TabsProps {
  children: React.ReactNode;
  value: string;
  onValueChange: (value: string) => void;
  className?: string;
}

interface TabsListProps {
  children: React.ReactNode;
  className?: string;
}

interface TabsTriggerProps {
  children: React.ReactNode;
  value: string;
  className?: string;
}

// Mock shadcn/ui Tabs components
jest.mock("@/components/ui/tabs", () => ({
  Tabs: ({ children, onValueChange, className, value }: TabsProps) => (
    <TabsContext.Provider value={{ onValueChange }}>
      <div data-testid="tabs" data-value={value} className={className}>
        {children}
      </div>
    </TabsContext.Provider>
  ),
  TabsList: ({ children, className }: TabsListProps) => (
    <div data-testid="tabs-list" className={className}>
      {children}
    </div>
  ),
  TabsTrigger: ({ children, value, className }: TabsTriggerProps) => {
    const ctx = React.useContext(TabsContext);
    return (
      <button
        data-testid="tabs-trigger"
        data-value={value}
        className={className}
        onClick={() => ctx?.onValueChange(value)}
      >
        {children}
      </button>
    );
  },
}));

type TranslationKey =
  | "statusAll"
  | "statusOpen"
  | "statusPending"
  | "statusClosed"
  | "statusError";

describe("StatusTabs", () => {
  const mockTranslations: Record<TranslationKey, string> = {
    statusAll: "All",
    statusOpen: "Open",
    statusPending: "Pending",
    statusClosed: "Closed",
    statusError: "Error",
  };

  beforeEach(() => {
    (useTranslations as jest.Mock).mockReturnValue(
      (key: TranslationKey) => mockTranslations[key],
    );
  });

  it("renders all tabs with correct labels", () => {
    const onChange = jest.fn();
    render(
      <StatusTabs currentTab="all" onChange={onChange}>
        <div>Test content</div>
      </StatusTabs>,
    );

    expect(screen.getByText("All")).toBeInTheDocument();
    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("Pending")).toBeInTheDocument();
    expect(screen.getByText("Closed")).toBeInTheDocument();
    expect(screen.getByText("Error")).toBeInTheDocument();
  });

  it("applies active styles to current tab", () => {
    const onChange = jest.fn();
    render(
      <StatusTabs currentTab="open" onChange={onChange}>
        <div>Test content</div>
      </StatusTabs>,
    );

    const openTab = screen.getByText("Open");
    expect(openTab).toHaveClass("bg-primary");
    expect(openTab).toHaveClass("text-primary-foreground");
  });

  it("calls onChange when tab is clicked", () => {
    const onChange = jest.fn();
    render(
      <StatusTabs currentTab="all" onChange={onChange}>
        <div>Test content</div>
      </StatusTabs>,
    );

    const openTab = screen.getByText("Open");
    fireEvent.click(openTab);
    expect(onChange).toHaveBeenCalledWith("open");
  });

  it("renders children content", () => {
    const onChange = jest.fn();
    render(
      <StatusTabs currentTab="all" onChange={onChange}>
        <div data-testid="test-content">Test content</div>
      </StatusTabs>,
    );

    expect(screen.getByTestId("test-content")).toBeInTheDocument();
  });

  it("handles all possible tab statuses", () => {
    const onChange = jest.fn();
    const tabStatuses: TabStatus[] = [
      "all",
      "open",
      "pending",
      "closed",
      "error",
    ];

    tabStatuses.forEach((status) => {
      const { unmount } = render(
        <StatusTabs currentTab={status} onChange={onChange}>
          <div>Test content</div>
        </StatusTabs>,
      );

      const translationKey =
        `status${status.charAt(0).toUpperCase() + status.slice(1)}` as TranslationKey;
      const activeTab = screen.getByText(mockTranslations[translationKey]);
      expect(activeTab).toHaveClass("bg-primary");
      expect(activeTab).toHaveClass("text-primary-foreground");

      unmount();
    });
  });
});
