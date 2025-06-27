import { apiClient } from "./client";
import { API_BASE_URL } from "./config";

export interface Notification {
  id: number;
  title: string;
  content: string;
  type: string;
  read: boolean;
  created_at: string;
}

export interface Pagination {
  current_page: number;
  total_pages: number;
  total_count: number;
  per_page: number;
}

export interface NotificationsResponse {
  status: string;
  data: {
    notifications: Notification[];
    pagination: Pagination;
  };
}

export interface MarkAsReadResponse {
  status: string;
  data: Notification;
}

export interface MarkAllReadResponse {
  status: string;
  message: string;
}

export const notificationsApi = {
  getNotifications: async (page: number, perPage: number) => {
    const response = await apiClient.get<NotificationsResponse>(
      `${API_BASE_URL}/notifications`,
      {
        params: { page, per_page: perPage },
      },
    );
    return response.data;
  },

  markAsRead: async (id: number) => {
    const response = await apiClient.put<MarkAsReadResponse>(
      `${API_BASE_URL}/notifications/${id}/read`,
      {},
    );
    return response.data;
  },

  markAllRead: async () => {
    const response = await apiClient.put<MarkAllReadResponse>(
      `${API_BASE_URL}/notifications/mark_all_read`,
      {},
    );
    return response.data;
  },
};
