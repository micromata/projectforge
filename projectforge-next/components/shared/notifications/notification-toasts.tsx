"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { toast } from "@/lib/toast";
import { useNotificationStore } from "@/store/notification-store";
import { NotificationCard } from "./notification-card";
import {
  useMyNotifications,
  useNotificationActions,
} from "./use-my-notifications";

/** How old the status may be on a page change before it's reloaded. */
const STATUS_MAX_AGE_MS = 10 * 1000;

function toastId(id: number): string {
  return `notification-${id}`;
}

/**
 * Shows the notifications displayed as toasts: a `TOAST` once (it counts as confirmed by being
 * shown, so it neither comes back nor escalates), a `TOAST_CONFIRM` until it is confirmed. Renders
 * nothing itself — the toasts live in the app's `Toaster`.
 *
 * Mounted once for the whole authenticated area (like JobToasts). Also reloads the menu whenever the
 * summary changes, as rules may count their notifications on a menu entry.
 */
export function NotificationToasts() {
  const notifications = useMyNotifications();
  const { acknowledge } = useNotificationActions();
  const { notifications: summary } = useAuth();
  const queryClient = useQueryClient();
  const { closedIds, closeNotification } = useNotificationStore();
  const pathname = usePathname();

  // A notification created meanwhile has to show up while moving through the app, not only after a
  // reload: a page change reloads the status (the summary), if it's older than a few seconds.
  useEffect(() => {
    const updatedAt =
      queryClient.getQueryState(["userStatus"])?.dataUpdatedAt ?? 0;
    if (Date.now() - updatedAt > STATUS_MAX_AGE_MS) {
      void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
    }
  }, [pathname, queryClient]);
  /** Toasts already raised in this tab: a refetch must not raise them again. */
  const shown = useRef(new Set<number>());
  /** The standing toasts to be confirmed: closed here once confirmed elsewhere (the bell, another tab). */
  const standing = useRef(new Set<number>());

  const summaryKey = summary ? `${summary.latestId}-${summary.openCount}` : "";
  const lastSummaryKey = useRef(summaryKey);
  useEffect(() => {
    if (summaryKey === lastSummaryKey.current) return;
    lastSummaryKey.current = summaryKey;
    void queryClient.invalidateQueries({ queryKey: ["menu"] });
  }, [summaryKey, queryClient]);

  useEffect(() => {
    for (const id of standing.current) {
      if (notifications.some((notification) => notification.id === id))
        continue;
      toast.dismiss(toastId(id));
      standing.current.delete(id);
    }
    for (const notification of notifications) {
      const { id, display } = notification;
      if (
        display === "BANNER" ||
        shown.current.has(id) ||
        closedIds.includes(id)
      )
        continue;
      shown.current.add(id);
      // Rendered by NotificationCard as in the bell, not as sonner's own toast (white, with the
      // action beside the text, which squeezed a longer text into a narrow column).
      // The tint of the severity is translucent: an opaque popover behind it, as in the bell.
      const card = () => (
        <div className="w-full rounded-md border bg-popover p-1 text-popover-foreground shadow-lg">
          <NotificationCard
            notification={notification}
            withActions={display === "TOAST_CONFIRM"}
            onOpen={() => toast.dismiss(toastId(id))}
            onClose={() => {
              toast.dismiss(toastId(id));
              closeNotification(id);
            }}
          />
        </div>
      );
      if (display === "TOAST") {
        toast.custom(card, { id: toastId(id) });
        acknowledge(id);
      } else {
        standing.current.add(id);
        toast.custom(card, {
          id: toastId(id),
          duration: Infinity,
          dismissible: false,
        });
      }
    }
  }, [notifications, acknowledge, closedIds, closeNotification]);

  return null;
}
