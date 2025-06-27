"use client";

import { useUserStore } from "@/lib/store/user-store";
import { useNotificationChannel } from "@/hooks/use-notification-channel";
import { toast, Toaster } from "sonner";
import { Notification } from "@/lib/api/notifications";
import { useNotificationStore } from "@/components/notification-bell";
import { useQueryClient } from "@tanstack/react-query";

// Định nghĩa interface cho response
interface NotificationsResponse {
  status: string;
  data: {
    notifications: Notification[];
    pagination: {
      current_page: number;
      total_pages: number;
      total_count: number;
      per_page: number;
    };
  };
}

export function NotificationProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = useUserStore((state) => state.user);
  const userId = user?.id ? Number(user.id) : 0;
  const setNewNotification = useNotificationStore(
    (state) => state.setNewNotification,
  );
  const queryClient = useQueryClient();

  // Sử dụng hook ở tất cả các render
  useNotificationChannel({
    userId,
    onNotificationReceived: (notification: Notification) => {
      // Show toast when receiving a new notification
      toast(notification.title, {
        description: notification.content,
        duration: 5000,
        position: "top-right",
        className: "shadow-lg border border-gray-200 dark:border-gray-800",
      });

      // Add new notification to the cache/store
      queryClient.setQueryData<NotificationsResponse>(
        ["notifications", 1],
        (oldData: NotificationsResponse | undefined) => {
          if (!oldData)
            return {
              status: "success",
              data: {
                notifications: [notification],
                pagination: {
                  current_page: 1,
                  total_pages: 1,
                  total_count: 1,
                  per_page: 20,
                },
              },
            };

          // Add new notification to the top of the list
          const newNotification = { ...notification, read: false };
          const newNotifications = [
            newNotification,
            ...oldData.data.notifications,
          ];

          return {
            ...oldData,
            data: {
              ...oldData.data,
              notifications: newNotifications,
              pagination: {
                ...oldData.data.pagination,
                total_count: newNotifications.length,
              },
            },
          };
        },
      );

      // Trigger animation on bell icon
      setNewNotification(true);
    },
  });

  return (
    <>
      <Toaster
        position="top-right"
        toastOptions={{
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
          style: {
            background: "var(--background)",
            color: "var(--foreground)",
            border: "1px solid var(--border)",
          },
          duration: 3000,
        }}
        closeButton
      />
      {children}
    </>
  );
}
