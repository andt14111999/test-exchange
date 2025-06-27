import * as React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

// Mock Radix UI Popover
jest.mock("@radix-ui/react-popover", () => {
  const MockContext = React.createContext<{
    isOpen: boolean;
    setIsOpen: (open: boolean) => void;
  }>({
    isOpen: false,
    setIsOpen: () => {},
  });

  const Root = ({
    children,
    open,
    defaultOpen,
    onOpenChange,
  }: {
    children: React.ReactNode;
    open?: boolean;
    defaultOpen?: boolean;
    onOpenChange?: (open: boolean) => void;
  }) => {
    const [isOpen, setIsOpen] = React.useState(open ?? defaultOpen ?? false);

    React.useEffect(() => {
      if (open !== undefined) {
        setIsOpen(open);
      }
    }, [open]);

    const handleOpenChange = React.useCallback(
      (newOpen: boolean) => {
        setIsOpen(newOpen);
        onOpenChange?.(newOpen);
      },
      [onOpenChange],
    );

    return (
      <MockContext.Provider value={{ isOpen, setIsOpen: handleOpenChange }}>
        {children}
      </MockContext.Provider>
    );
  };
  Root.displayName = "PopoverRoot";

  const Trigger = ({ children }: { children: React.ReactNode }) => {
    const { isOpen, setIsOpen } = React.useContext(MockContext);
    return <button onClick={() => setIsOpen(!isOpen)}>{children}</button>;
  };
  Trigger.displayName = "PopoverTrigger";

  const Portal = ({ children }: { children: React.ReactNode }) => {
    const { isOpen } = React.useContext(MockContext);
    if (!isOpen) return null;
    return <>{children}</>;
  };
  Portal.displayName = "PopoverPortal";

  const Content = React.forwardRef<
    HTMLDivElement,
    React.ComponentPropsWithoutRef<"div"> & {
      sideOffset?: number;
      align?: string;
    }
  >(({ children, sideOffset, align = "center", ...props }, ref) => {
    const { isOpen, setIsOpen } = React.useContext(MockContext);

    React.useEffect(() => {
      const handleClickOutside = (e: MouseEvent) => {
        const target = e.target as HTMLElement;
        if (
          !target.closest("[data-radix-popover-content]") &&
          target.tagName !== "BUTTON"
        ) {
          setIsOpen(false);
        }
      };

      if (isOpen) {
        document.addEventListener("click", handleClickOutside);
        return () => document.removeEventListener("click", handleClickOutside);
      }
    }, [isOpen, setIsOpen]);

    if (!isOpen) return null;

    return (
      <div
        ref={ref}
        data-side-offset={sideOffset}
        data-align={align}
        data-radix-popover-content
        {...props}
      >
        {children}
      </div>
    );
  });
  Content.displayName = "PopoverContent";

  return { Root, Trigger, Portal, Content };
});

describe("Popover", () => {
  const renderPopover = (props = {}) => {
    return render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent {...props}>
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );
  };

  it("should render trigger button", () => {
    renderPopover();
    expect(screen.getByText("Click me")).toBeInTheDocument();
  });

  it("should show content when trigger is clicked", async () => {
    renderPopover();
    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);
    expect(screen.getByText("Popover content")).toBeInTheDocument();
  });

  it("should hide content when clicking outside", async () => {
    renderPopover();
    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);
    expect(screen.getByText("Popover content")).toBeInTheDocument();

    // Click outside
    await userEvent.click(document.body);
    expect(screen.queryByText("Popover content")).not.toBeInTheDocument();
  });

  it("should apply custom className to PopoverContent", async () => {
    render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent className="custom-class">
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );

    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    const content = screen.getByText("Popover content").parentElement;
    expect(content).toHaveClass("custom-class");
  });

  it("should apply default align prop to PopoverContent", async () => {
    renderPopover();
    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    const content = screen.getByText("Popover content").parentElement;
    expect(content).toHaveAttribute("data-align", "center");
  });

  it("should apply custom align prop to PopoverContent", async () => {
    render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent align="start">
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );

    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    const content = screen.getByText("Popover content").parentElement;
    expect(content).toHaveAttribute("data-align", "start");
  });

  it("should apply default sideOffset prop to PopoverContent", async () => {
    renderPopover();
    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    const content = screen.getByText("Popover content").parentElement;
    expect(content).toHaveAttribute("data-side-offset", "4");
  });

  it("should apply custom sideOffset prop to PopoverContent", async () => {
    render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent sideOffset={8}>
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );

    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    const content = screen.getByText("Popover content").parentElement;
    expect(content).toHaveAttribute("data-side-offset", "8");
  });

  it("should forward ref to PopoverContent", async () => {
    const ref = React.createRef<HTMLDivElement>();
    render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent ref={ref}>
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );

    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    expect(ref.current).toBeTruthy();
  });

  it("should handle additional props passed to PopoverContent", async () => {
    render(
      <Popover>
        <PopoverTrigger>Click me</PopoverTrigger>
        <PopoverContent data-testid="popover-content">
          <div>Popover content</div>
        </PopoverContent>
      </Popover>,
    );

    const trigger = screen.getByText("Click me");
    await userEvent.click(trigger);

    expect(screen.getByTestId("popover-content")).toBeInTheDocument();
  });

  it("should have correct displayName", () => {
    expect(PopoverContent.displayName).toBe("PopoverContent");
  });
});
