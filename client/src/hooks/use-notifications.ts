import { notificationsApi } from "@/lib/api/notifications";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export function useNotifications(page = 1, perPage = 20) {
  return useQuery({
    queryKey: ["notifications", page],
    queryFn: () => notificationsApi.getNotifications(page, perPage),
  });
}

export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useMarkAllRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}
