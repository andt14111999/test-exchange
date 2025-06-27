import { renderHook, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useNotifications,
  useMarkAsRead,
  useMarkAllRead,
} from "@/hooks/use-notifications";
import { notificationsApi } from "@/lib/api/notifications";
import React from "react";

// Mock the notifications API
jest.mock("@/lib/api/notifications", () => ({
  notificationsApi: {
    getNotifications: jest.fn(),
    markAsRead: jest.fn(),
    markAllRead: jest.fn(),
  },
}));

// Mock next-intl and its dependencies
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

jest.mock("next-intl/server", () => ({
  getRequestConfig: jest.fn(),
  getMessages: jest.fn(),
  getLocale: jest.fn(),
  getTimeZone: jest.fn(),
}));

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        staleTime: Infinity,
      },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Notifications Hooks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("useNotifications", () => {
    const mockNotifications = [
      { id: 1, message: "Test notification 1" },
      { id: 2, message: "Test notification 2" },
    ];

    beforeEach(() => {
      (notificationsApi.getNotifications as jest.Mock).mockImplementation(() =>
        Promise.resolve(mockNotifications),
      );
    });

    it("should fetch notifications with default pagination", async () => {
      const { result } = renderHook(() => useNotifications(), {
        wrapper: createWrapper()[0],
      });

      // Initial state should be loading
      expect(result.current.isLoading).toBe(true);
      expect(result.current.data).toBeUndefined();

      // Wait for the query to resolve
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      // Ensure the query has settled
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      // Check if API was called with default parameters
      expect(notificationsApi.getNotifications).toHaveBeenCalledWith(1, 20);
      expect(result.current.data).toEqual(mockNotifications);
      expect(result.current.isLoading).toBe(false);
    });

    it("should fetch notifications with custom pagination", async () => {
      const { result } = renderHook(() => useNotifications(2, 10), {
        wrapper: createWrapper()[0],
      });

      // Wait for initial query to settle
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      // Ensure the query has completely settled
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(notificationsApi.getNotifications).toHaveBeenCalledWith(2, 10);
      expect(result.current.data).toEqual(mockNotifications);
      expect(result.current.isLoading).toBe(false);
    });

    it("should handle error state", async () => {
      const error = new Error("Failed to fetch notifications");
      (notificationsApi.getNotifications as jest.Mock).mockRejectedValueOnce(
        error,
      );

      const { result } = renderHook(() => useNotifications(), {
        wrapper: createWrapper()[0],
      });

      // Initial state
      expect(result.current.isLoading).toBe(true);
      expect(result.current.isError).toBe(false);
      expect(result.current.error).toBeNull();

      // Wait for the query to reject
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      // Ensure the query has settled
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(result.current.isLoading).toBe(false);
      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should use cached data when available", async () => {
      const [Wrapper, queryClient] = createWrapper();

      // Prefetch data
      await queryClient.prefetchQuery({
        queryKey: ["notifications", 1],
        queryFn: () => Promise.resolve(mockNotifications),
      });

      // Reset the mock to verify it's not called again
      (notificationsApi.getNotifications as jest.Mock).mockClear();

      const { result } = renderHook(() => useNotifications(), {
        wrapper: Wrapper,
      });

      // Data should be immediately available without loading state
      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(mockNotifications);
      expect(notificationsApi.getNotifications).not.toHaveBeenCalled();
    });

    it("should refetch when page changes", async () => {
      (notificationsApi.getNotifications as jest.Mock).mockImplementation(
        (page) => Promise.resolve([{ id: page, message: `Page ${page}` }]),
      );

      const [Wrapper] = createWrapper();
      const { result, rerender } = renderHook(
        ({ page }) => useNotifications(page, 20),
        {
          wrapper: Wrapper,
          initialProps: { page: 1 },
        },
      );

      // Wait for initial query to settle
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      // Verify initial data
      expect(result.current.data).toEqual([{ id: 1, message: "Page 1" }]);

      // Change page
      rerender({ page: 2 });

      // Wait for refetch to complete
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      // Ensure the query has completely settled
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(notificationsApi.getNotifications).toHaveBeenCalledTimes(2);
      expect(notificationsApi.getNotifications).toHaveBeenLastCalledWith(2, 20);
      expect(result.current.data).toEqual([{ id: 2, message: "Page 2" }]);
    });
  });

  describe("useMarkAsRead", () => {
    const mockQueryClient = {
      invalidateQueries: jest.fn(),
    };

    beforeEach(() => {
      jest
        .spyOn(QueryClient.prototype, "invalidateQueries")
        .mockImplementation(mockQueryClient.invalidateQueries);
    });

    it("should mark notification as read and invalidate queries", async () => {
      (notificationsApi.markAsRead as jest.Mock).mockResolvedValue({
        success: true,
      });

      const { result } = renderHook(() => useMarkAsRead(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        await result.current.mutateAsync(1);
      });

      expect(notificationsApi.markAsRead).toHaveBeenCalledWith(1);
      expect(mockQueryClient.invalidateQueries).toHaveBeenCalledWith({
        queryKey: ["notifications"],
      });
    });

    it("should handle error when marking as read", async () => {
      const error = new Error("Failed to mark as read");
      (notificationsApi.markAsRead as jest.Mock).mockRejectedValue(error);

      const { result } = renderHook(() => useMarkAsRead(), {
        wrapper: createWrapper()[0],
      });

      let caught: unknown = null;
      await act(async () => {
        try {
          await result.current.mutateAsync(1);
        } catch (err) {
          caught = err;
        }
      });

      // Wait for mutation state to update
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(caught).toBe(error);
      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should handle concurrent mark as read mutations", async () => {
      const [Wrapper] = createWrapper();
      const notificationIds = [1, 2];

      (notificationsApi.markAsRead as jest.Mock).mockImplementation((id) =>
        Promise.resolve({ success: true, id }),
      );

      const { result } = renderHook(() => useMarkAsRead(), {
        wrapper: Wrapper,
      });

      await act(async () => {
        const promises = notificationIds.map((id) =>
          result.current.mutateAsync(id),
        );
        await Promise.all(promises);
      });

      expect(notificationsApi.markAsRead).toHaveBeenCalledTimes(2);
      expect(notificationsApi.markAsRead).toHaveBeenNthCalledWith(1, 1);
      expect(notificationsApi.markAsRead).toHaveBeenNthCalledWith(2, 2);
    });
  });

  describe("useMarkAllRead", () => {
    const mockQueryClient = {
      invalidateQueries: jest.fn(),
    };

    beforeEach(() => {
      jest
        .spyOn(QueryClient.prototype, "invalidateQueries")
        .mockImplementation(mockQueryClient.invalidateQueries);
    });

    it("should mark all notifications as read and invalidate queries", async () => {
      (notificationsApi.markAllRead as jest.Mock).mockResolvedValue({
        success: true,
      });

      const { result } = renderHook(() => useMarkAllRead(), {
        wrapper: createWrapper()[0],
      });

      await act(async () => {
        await result.current.mutateAsync();
      });

      expect(notificationsApi.markAllRead).toHaveBeenCalled();
      expect(mockQueryClient.invalidateQueries).toHaveBeenCalledWith({
        queryKey: ["notifications"],
      });
    });

    it("should handle error when marking all as read", async () => {
      const error = new Error("Failed to mark all as read");
      (notificationsApi.markAllRead as jest.Mock).mockRejectedValue(error);

      const { result } = renderHook(() => useMarkAllRead(), {
        wrapper: createWrapper()[0],
      });

      let caught: unknown = null;
      await act(async () => {
        try {
          await result.current.mutateAsync();
        } catch (err) {
          caught = err;
        }
      });

      // Wait for mutation state to update
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(caught).toBe(error);
      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(error);
    });

    it("should handle specific API error types", async () => {
      const apiError = {
        code: "UNAUTHORIZED",
        message: "User not authorized to mark all notifications as read",
      };
      (notificationsApi.markAllRead as jest.Mock).mockRejectedValue(apiError);

      const { result } = renderHook(() => useMarkAllRead(), {
        wrapper: createWrapper()[0],
      });

      let caught: unknown = null;
      await act(async () => {
        try {
          await result.current.mutateAsync();
        } catch (err) {
          caught = err;
        }
      });

      // Wait for mutation state to update
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(caught).toBe(apiError);
      expect(result.current.isError).toBe(true);
      expect(result.current.error).toEqual(apiError);
    });
  });
});
