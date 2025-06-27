import { apiClient } from "@/lib/api/client";
import { API_BASE_URL } from "@/lib/api/config";
import {
  notificationsApi,
  type NotificationsResponse,
  type MarkAsReadResponse,
  type MarkAllReadResponse,
  type Notification,
} from "@/lib/api/notifications";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

describe("Notifications API", () => {
  const mockNotification: Notification = {
    id: 1,
    title: "Test Notification",
    content: "This is a test notification",
    type: "info",
    read: false,
    created_at: "2024-03-20T00:00:00Z",
  };

  const mockPagination = {
    current_page: 1,
    total_pages: 5,
    total_count: 50,
    per_page: 10,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getNotifications", () => {
    it("should fetch notifications successfully", async () => {
      const mockResponse: { data: NotificationsResponse } = {
        data: {
          status: "success",
          data: {
            notifications: [mockNotification],
            pagination: mockPagination,
          },
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await notificationsApi.getNotifications(1, 10);

      expect(apiClient.get).toHaveBeenCalledWith(
        `${API_BASE_URL}/notifications`,
        {
          params: { page: 1, per_page: 10 },
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when fetching notifications", async () => {
      const mockError = new Error("Failed to fetch notifications");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(notificationsApi.getNotifications(1, 10)).rejects.toThrow(
        mockError,
      );
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("markAsRead", () => {
    it("should mark notification as read successfully", async () => {
      const mockResponse: { data: MarkAsReadResponse } = {
        data: {
          status: "success",
          data: { ...mockNotification, read: true },
        },
      };

      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await notificationsApi.markAsRead(1);

      expect(apiClient.put).toHaveBeenCalledWith(
        `${API_BASE_URL}/notifications/1/read`,
        {},
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when marking notification as read", async () => {
      const mockError = new Error("Failed to mark notification as read");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(notificationsApi.markAsRead(1)).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("markAllRead", () => {
    it("should mark all notifications as read successfully", async () => {
      const mockResponse: { data: MarkAllReadResponse } = {
        data: {
          status: "success",
          message: "All notifications marked as read",
        },
      };

      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await notificationsApi.markAllRead();

      expect(apiClient.put).toHaveBeenCalledWith(
        `${API_BASE_URL}/notifications/mark_all_read`,
        {},
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error when marking all notifications as read", async () => {
      const mockError = new Error("Failed to mark all notifications as read");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(notificationsApi.markAllRead()).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });
});
