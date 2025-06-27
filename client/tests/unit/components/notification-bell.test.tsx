import {
  render,
  screen,
  fireEvent,
  act,
  waitFor,
} from "@testing-library/react";
import {
  NotificationBell,
  useNotificationStore,
} from "@/components/notification-bell";
import * as hooks from "@/hooks/use-notifications";
import type { ReactNode } from "react";

// Mock the hooks
jest.mock("@/hooks/use-notifications");
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

// Mock UI components
jest.mock("@/components/ui/badge", () => ({
  Badge: ({
    children,
    className,
    variant,
  }: {
    children: ReactNode;
    className?: string;
    variant?: string;
  }) => (
    <span className={className} data-variant={variant} data-testid="badge">
      {children}
    </span>
  ),
}));

jest.mock("@/components/ui/button", () => ({
  Button: ({
    children,
    onClick,
    className,
    disabled,
    variant,
    size,
  }: {
    children: ReactNode;
    onClick?: () => void;
    className?: string;
    disabled?: boolean;
    variant?: string;
    size?: string;
  }) => (
    <button
      onClick={onClick}
      className={className}
      disabled={disabled}
      data-variant={variant}
      data-size={size}
      data-testid="button"
    >
      {children}
    </button>
  ),
}));

jest.mock("@/components/ui/popover", () => ({
  Popover: ({
    children,
    onOpenChange,
  }: {
    children: ReactNode;
    onOpenChange?: (open: boolean) => void;
  }) => {
    return (
      <div data-testid="popover" onClick={() => onOpenChange?.(true)}>
        {children}
      </div>
    );
  },
  PopoverTrigger: ({
    children,
    asChild,
  }: {
    children: ReactNode;
    asChild?: boolean;
  }) => (
    <div data-testid="popover-trigger" data-as-child={asChild}>
      {children}
    </div>
  ),
  PopoverContent: ({
    children,
    className,
    align,
  }: {
    children: ReactNode;
    className?: string;
    align?: string;
  }) => (
    <div data-testid="popover-content" className={className} data-align={align}>
      {children}
    </div>
  ),
}));

// Mock Lucide icon
jest.mock("lucide-react", () => ({
  Bell: () => <div data-testid="bell-icon">Bell Icon</div>,
}));

// Mock data
const mockNotifications = [
  {
    id: 1,
    title: "Test Notification 1",
    content: "Test Content 1",
    read: false,
  },
  {
    id: 2,
    title: "Test Notification 2",
    content: "Test Content 2",
    read: true,
  },
];

const mockPagination = {
  total_pages: 2,
  current_page: 1,
};

describe("NotificationBell", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Mock default hook values
    (hooks.useNotifications as jest.Mock).mockReturnValue({
      data: {
        data: {
          notifications: mockNotifications,
          pagination: mockPagination,
        },
      },
      isLoading: false,
      refetch: jest.fn(),
    });

    (hooks.useMarkAsRead as jest.Mock).mockReturnValue({
      mutateAsync: jest.fn().mockResolvedValue({}),
      isPending: false,
    });

    (hooks.useMarkAllRead as jest.Mock).mockReturnValue({
      mutateAsync: jest.fn().mockResolvedValue({}),
      isPending: false,
    });
  });

  it("renders notification bell with unread count", () => {
    render(<NotificationBell />);

    // Check if bell icon is rendered
    expect(screen.getByTestId("bell-icon")).toBeInTheDocument();

    // Check if unread count badge is rendered
    const badge = screen.getByTestId("badge");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent("1");
  });

  it("opens popover and displays notifications when clicked", async () => {
    render(<NotificationBell />);

    // Click the bell icon
    fireEvent.click(screen.getByTestId("popover"));

    // Check if notifications are displayed
    expect(screen.getByText("Test Notification 1")).toBeInTheDocument();
    expect(screen.getByText("Test Content 1")).toBeInTheDocument();
    expect(screen.getByText("Test Notification 2")).toBeInTheDocument();
    expect(screen.getByText("Test Content 2")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    (hooks.useNotifications as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
      refetch: jest.fn(),
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));
    expect(screen.getByText("notifications.loading")).toBeInTheDocument();
  });

  it("shows empty state when no notifications", () => {
    (hooks.useNotifications as jest.Mock).mockReturnValue({
      data: { data: { notifications: [], pagination: { total_pages: 0 } } },
      isLoading: false,
      refetch: jest.fn(),
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));
    expect(
      screen.getByText("notifications.noNotifications"),
    ).toBeInTheDocument();
  });

  it("marks notification as read when clicking check button", async () => {
    const mockMarkAsRead = jest.fn().mockResolvedValue({});
    (hooks.useMarkAsRead as jest.Mock).mockReturnValue({
      mutateAsync: mockMarkAsRead,
      isPending: false,
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));

    // Click the mark as read button for the first notification
    const markAsReadButton = screen.getByRole("button", {
      name: "notifications.markAsRead",
    });
    await act(async () => {
      fireEvent.click(markAsReadButton);
    });

    expect(mockMarkAsRead).toHaveBeenCalledWith(1);
  });

  it("marks all notifications as read", async () => {
    const mockMarkAllRead = jest.fn().mockResolvedValue({});
    (hooks.useMarkAllRead as jest.Mock).mockReturnValue({
      mutateAsync: mockMarkAllRead,
      isPending: false,
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));

    // Click the mark all as read button
    const markAllReadButton = screen.getByText("notifications.markAllAsRead");
    await act(async () => {
      fireEvent.click(markAllReadButton);
    });

    expect(mockMarkAllRead).toHaveBeenCalled();
  });

  it("loads more notifications when clicking load more button", async () => {
    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));

    // Click the load more button
    const loadMoreButton = screen.getByText("notifications.loadMore");
    await act(async () => {
      fireEvent.click(loadMoreButton);
    });

    // Verify that the page state was updated
    expect(hooks.useNotifications).toHaveBeenCalledWith(2);
  });

  it("handles new notification animation", async () => {
    render(<NotificationBell />);
    const bellButton = screen
      .getByTestId("popover-trigger")
      .querySelector("button");

    // Initially no animation
    expect(bellButton).not.toHaveClass("animate-bounce");

    // Simulate new notification
    act(() => {
      useNotificationStore.getState().setNewNotification(true);
    });

    // Check if animation class is added
    expect(bellButton).toHaveClass("animate-bounce");

    // Fast forward time
    act(() => {
      jest.advanceTimersByTime(1000);
    });

    // Wait for the animation class to be removed
    await waitFor(() => {
      expect(bellButton).not.toHaveClass("animate-bounce");
    });
  });

  it("disables mark as read button when pending", async () => {
    (hooks.useMarkAsRead as jest.Mock).mockReturnValue({
      mutateAsync: jest.fn(),
      isPending: true,
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));

    const markAsReadButton = screen.getByRole("button", {
      name: "notifications.markAsRead",
    });
    expect(markAsReadButton).toBeDisabled();
  });

  it("disables mark all as read button when pending", async () => {
    (hooks.useMarkAllRead as jest.Mock).mockReturnValue({
      mutateAsync: jest.fn(),
      isPending: true,
    });

    render(<NotificationBell />);

    fireEvent.click(screen.getByTestId("popover"));

    const markAllReadButton = screen.getByText("notifications.markAllAsRead");
    expect(markAllReadButton).toBeDisabled();
  });
});
