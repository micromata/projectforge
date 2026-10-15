"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useAuth } from "@/hooks/use-auth";
import {
  acknowledgeNotification,
  fetchMyNotifications,
  markNotificationDone,
  type AppNotification,
} from "@/lib/rs/notification";

/**
 * The notifications of the logged-in user visible in the app (toasts, banners, the bell).
 *
 * Loaded only when the summary polled with the user status (see useAuth) says there are any, and
 * again whenever it changes: the summary is part of the query key, so a new notification (higher
 * `latestId`) or a finished one (lower `openCount`) refetches without a timer of its own.
 */
export function useMyNotifications(): AppNotification[] {
  const { notifications: summary } = useAuth();
  const openCount = summary?.openCount ?? 0;
  const { data } = useQuery({
    queryKey: ["notifications", summary?.latestId ?? null, openCount],
    queryFn: ({ signal }) => fetchMyNotifications(signal),
    enabled: openCount > 0,
    // The old list until the new one is there: an empty one in between would close standing toasts.
    placeholderData: keepPreviousData,
  });
  return openCount > 0 ? (data ?? []) : [];
}

/**
 * Confirming and marking as done. Afterwards the user status (summary), the list and the menu
 * (badge counters of the rules) are reloaded: a confirmed banner keeps the summary unchanged, so the
 * list is invalidated on its own.
 */
export function useNotificationActions() {
  const queryClient = useQueryClient();
  const onSettled = () => {
    void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
    void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    void queryClient.invalidateQueries({ queryKey: ["menu"] });
  };
  const acknowledge = useMutation({
    mutationFn: (id: number) => acknowledgeNotification(id),
    onSettled,
  });
  const done = useMutation({
    mutationFn: (id: number) => markNotificationDone(id),
    onSettled,
  });
  return {
    acknowledge: (id: number) => acknowledge.mutate(id),
    done: (id: number) => done.mutate(id),
    isPending: acknowledge.isPending || done.isPending,
  };
}
