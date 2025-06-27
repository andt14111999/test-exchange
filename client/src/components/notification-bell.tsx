"use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  useMarkAllRead,
  useMarkAsRead,
  useNotifications,
} from "@/hooks/use-notifications";
import type { Notification } from "@/lib/api/notifications";
import { Bell } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState, useEffect } from "react";
import { create } from "zustand";

interface NotificationStore {
  newNotification: boolean;
  setNewNotification: (value: boolean) => void;
}

export const useNotificationStore = create<NotificationStore>((set) => ({
  newNotification: false,
  setNewNotification: (value) => set({ newNotification: value }),
}));

export function NotificationBell() {
  const t = useTranslations();
  const [page, setPage] = useState(1);
  const { data, isLoading } = useNotifications(page);
  const markAsRead = useMarkAsRead();
  const markAllRead = useMarkAllRead();
  const newNotification = useNotificationStore(
    (state) => state.newNotification,
  );
  const setNewNotification = useNotificationStore(
    (state) => state.setNewNotification,
  );
  const [isAnimating, setIsAnimating] = useState(false);

  const notifications = data?.data.notifications || [];
  const unreadCount = notifications.filter((n: Notification) => !n.read).length;

  const handleMarkAsRead = async (id: number) => {
    await markAsRead.mutateAsync(id);
  };

  const handleMarkAllRead = async () => {
    await markAllRead.mutateAsync();
  };

  useEffect(() => {
    if (newNotification) {
      setIsAnimating(true);
      setTimeout(() => {
        setIsAnimating(false);
      }, 1000);
    }
  }, [newNotification]);

  const handleOpenChange = (open: boolean) => {
    if (open) {
      setNewNotification(false);
    }
  };

  return (
    <Popover onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className={`relative ${isAnimating ? "animate-bounce" : ""}`}
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -bottom-1 -right-1 h-5 w-5 rounded-full p-0 text-xs flex items-center justify-center"
            >
              {unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <div className="flex items-center justify-between border-b px-4 py-2">
          <h4 className="font-semibold">{t("notifications.title")}</h4>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto p-0 text-xs"
              onClick={handleMarkAllRead}
              disabled={markAllRead.isPending}
            >
              {t("notifications.markAllAsRead")}
            </Button>
          )}
        </div>
        <div className="max-h-[400px] overflow-y-auto">
          {isLoading ? (
            <div className="p-4 text-center text-sm text-muted-foreground">
              {t("notifications.loading")}
            </div>
          ) : notifications.length === 0 ? (
            <div className="p-4 text-center text-sm text-muted-foreground">
              {t("notifications.noNotifications")}
            </div>
          ) : (
            notifications.map((notification: Notification) => (
              <div
                key={notification.id}
                className={`flex items-start gap-4 border-b p-4 ${
                  !notification.read ? "bg-muted/50" : ""
                }`}
              >
                <div className="flex-1 space-y-1">
                  <p className="text-sm font-medium leading-none">
                    {notification.title}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {notification.content}
                  </p>
                </div>
                {!notification.read && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={() => handleMarkAsRead(notification.id)}
                    disabled={markAsRead.isPending}
                  >
                    <span className="sr-only">
                      {t("notifications.markAsRead")}
                    </span>
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className="h-4 w-4"
                    >
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                  </Button>
                )}
              </div>
            ))
          )}
        </div>
        {(data?.data?.pagination?.total_pages ?? 0) > 1 && (
          <div className="border-t p-2 text-center">
            <Button
              variant="ghost"
              size="sm"
              className="w-full"
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= (data?.data?.pagination?.total_pages ?? 1)}
            >
              {t("notifications.loadMore")}
            </Button>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
