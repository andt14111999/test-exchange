import { render, act } from "@testing-library/react";
import { NotificationProvider } from "@/components/providers/notification-provider";
import { useNotificationChannel } from "@/hooks/use-notification-channel";
import { useUserStore } from "@/lib/store/user-store";
import { useNotificationStore } from "@/components/notification-bell";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { Notification } from "@/lib/api/notifications";

// Mock all the hooks and modules
jest.mock("@/hooks/use-notification-channel");
jest.mock("@/lib/store/user-store");
jest.mock("@/components/notification-bell");
jest.mock("@tanstack/react-query");
jest.mock("sonner", () => ({
  toast: jest.fn(),
  Toaster: () => null,
}));

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

describe("NotificationProvider", () => {
  // Mock implementations
  const mockSetNewNotification = jest.fn();
  const mockSetQueryData = jest.fn();
  const mockUser = { id: "123" };

  beforeEach(() => {
    jest.clearAllMocks();

    // Mock useUserStore
    (useUserStore as unknown as jest.Mock).mockImplementation((selector) =>
      selector({ user: mockUser }),
    );

    // Mock useNotificationStore
    (useNotificationStore as unknown as jest.Mock).mockImplementation(
      (selector) =>
        selector({
          setNewNotification: mockSetNewNotification,
        }),
    );

    // Mock useQueryClient
    (useQueryClient as jest.Mock).mockReturnValue({
      setQueryData: mockSetQueryData,
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should render children correctly", () => {
    const { getByText } = render(
      <NotificationProvider>
        <div>Test Child</div>
      </NotificationProvider>,
    );

    expect(getByText("Test Child")).toBeInTheDocument();
  });

  it("should initialize notification channel with correct userId", () => {
    render(
      <NotificationProvider>
        <div>Test Child</div>
      </NotificationProvider>,
    );

    expect(useNotificationChannel).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 123,
      }),
    );
  });

  it("should handle notification updates correctly", () => {
    // Mock notification data
    const mockNotification: Notification = {
      id: 1,
      title: "Test Notification",
      content: "Test Content",
      type: "info",
      read: false,
      created_at: "2024-03-20T00:00:00Z",
    };

    // Capture the onNotificationReceived callback
    let onNotificationReceived: (notification: Notification) => void;
    (useNotificationChannel as jest.Mock).mockImplementation(
      ({ onNotificationReceived: callback }) => {
        onNotificationReceived = callback;
      },
    );

    render(
      <NotificationProvider>
        <div>Test Child</div>
      </NotificationProvider>,
    );

    // Simulate notification received
    act(() => {
      onNotificationReceived(mockNotification);
    });

    // Verify toast was shown
    expect(toast).toHaveBeenCalledWith(mockNotification.title, {
      description: mockNotification.content,
      duration: 5000,
      position: "top-right",
      className: "shadow-lg border border-gray-200 dark:border-gray-800",
    });

    // Verify notification bell was updated
    expect(mockSetNewNotification).toHaveBeenCalledWith(true);

    // Verify query data was updated
    expect(mockSetQueryData).toHaveBeenCalledWith(
      ["notifications", 1],
      expect.any(Function),
    );

    // Test the query data transformer with no existing data
    const transformer = mockSetQueryData.mock.calls[0][1];
    const newDataResult = transformer(undefined);
    expect(newDataResult).toEqual({
      status: "success",
      data: {
        notifications: [mockNotification],
        pagination: {
          current_page: 1,
          total_pages: 1,
          total_count: 1,
          per_page: 20,
        },
      },
    });

    // Test with existing data
    const existingData = {
      status: "success",
      data: {
        notifications: [
          {
            id: 2,
            title: "Old Notification",
            content: "Old Content",
            type: "info",
            read: true,
            created_at: "2024-03-19T00:00:00Z",
          },
        ],
        pagination: {
          current_page: 1,
          total_pages: 1,
          total_count: 1,
          per_page: 20,
        },
      },
    };

    const updatedDataResult = transformer(existingData);
    expect(updatedDataResult).toEqual({
      status: "success",
      data: {
        notifications: [
          { ...mockNotification, read: false },
          ...existingData.data.notifications,
        ],
        pagination: {
          ...existingData.data.pagination,
          total_count: 2,
        },
      },
    });
  });

  it("should handle user with no ID correctly", () => {
    // Mock user with no ID
    (useUserStore as unknown as jest.Mock).mockImplementation((selector) =>
      selector({ user: null }),
    );

    render(
      <NotificationProvider>
        <div>Test Child</div>
      </NotificationProvider>,
    );

    expect(useNotificationChannel).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 0,
      }),
    );
  });
});
