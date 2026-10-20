"use client";

import { useEffect, useMemo, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import { useNotificationStore } from "@/store/notification-store";
import { NotificationCard } from "./notification-card";
import { bySeverity } from "./severity";
import {
  useMyNotifications,
  useNotificationActions,
} from "./use-my-notifications";

/** How old the status may be on a page change before it's reloaded. */
const STATUS_MAX_AGE_MS = 10 * 1000;

/** The one toast of all notifications to be confirmed. */
const STANDING_TOAST_ID = "notification-standing";

/** The frame of a toast: an opaque popover, as the bell's menu. */
const CARD_FRAME =
  "w-full rounded-md border bg-popover p-1 text-popover-foreground shadow-lg";

function toastId(id: number): string {
  return `notification-${id}`;
}

/**
 * Shows the notifications displayed as toasts: a `TOAST` once (it counts as confirmed by being
 * shown, so it neither comes back nor escalates), a `TOAST_CONFIRM` until it is confirmed — all of
 * these together in one toast, underneath each other. Renders nothing itself — the toasts live in the app's `Toaster`.
 *
 * Mounted once for the whole authenticated area (like JobToasts). Also reloads the menu whenever the
 * summary changes, as rules may count their notifications on a menu entry.
 */
export function NotificationToasts() {
  const notifications = useMyNotifications();
  const { acknowledge } = useNotificationActions();
  const { notifications: summary } = useAuth();
  const queryClient = useQueryClient();
  const { closedIds, closeNotification, bellOpen } = useNotificationStore();
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
  /** The one-shot toasts already raised in this tab: a refetch must not raise them again. */
  const shown = useRef(new Set<number>());

  const summaryKey = summary ? `${summary.latestId}-${summary.openCount}` : "";
  const lastSummaryKey = useRef(summaryKey);
  useEffect(() => {
    if (summaryKey === lastSummaryKey.current) return;
    lastSummaryKey.current = summaryKey;
    void queryClient.invalidateQueries({ queryKey: ["menu"] });
  }, [summaryKey, queryClient]);

  useEffect(() => {
    for (const notification of notifications) {
      const { id, display } = notification;
      if (
        display !== "TOAST" ||
        shown.current.has(id) ||
        closedIds.includes(id)
      )
        continue;
      shown.current.add(id);
      // Rendered by NotificationCard as in the bell, not as sonner's own toast (white, with the
      // action beside the text, which squeezed a longer text into a narrow column).
      // The tint of the severity is translucent: an opaque popover behind it, as in the bell.
      toast.custom(
        () => (
          <div className={CARD_FRAME}>
            <NotificationCard
              notification={notification}
              withActions={false}
              onOpen={() => toast.dismiss(toastId(id))}
              onClose={() => {
                toast.dismiss(toastId(id));
                closeNotification(id);
              }}
            />
          </div>
        ),
        { id: toastId(id) }
      );
      acknowledge(id);
    }
  }, [notifications, acknowledge, closedIds, closeNotification]);

  // All notifications to be confirmed share one toast, underneath each other: as toasts of their own,
  // sonner stacked them and showed the ones behind the front one only on hover. Raised again with the
  // same id on every change, which updates it in place; one confirmed (here, in the bell or in another
  // tab) or closed simply drops out of the list. Hidden while the bell is open, which lists them all
  // and whose menu it would cover.
  const standing = useMemo(
    () =>
      bySeverity(
        notifications.filter(
          ({ id, display }) =>
            display === "TOAST_CONFIRM" && !closedIds.includes(id)
        )
      ),
    [notifications, closedIds]
  );
  useEffect(() => {
    if (standing.length === 0 || bellOpen) {
      toast.dismiss(STANDING_TOAST_ID);
      return;
    }
    toast.custom(
      () => (
        <div
          className={cn(CARD_FRAME, "max-h-[70vh] space-y-1 overflow-y-auto")}
        >
          {standing.map((notification) => (
            <NotificationCard
              key={notification.id}
              notification={notification}
              onOpen={() => closeNotification(notification.id)}
              onClose={() => closeNotification(notification.id)}
            />
          ))}
        </div>
      ),
      { id: STANDING_TOAST_ID, duration: Infinity, dismissible: false }
    );
  }, [standing, bellOpen, closeNotification]);

  return null;
}
