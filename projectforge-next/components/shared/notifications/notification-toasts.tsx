"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/use-auth";
import { toast } from "@/lib/toast";
import { NotificationBody } from "./notification-body";
import {
  useMyNotifications,
  useNotificationActions,
} from "./use-my-notifications";

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
  const t = useTranslations("notification");
  const notifications = useMyNotifications();
  const { acknowledge } = useNotificationActions();
  const { notifications: summary } = useAuth();
  const queryClient = useQueryClient();
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
      const { id, display, title } = notification;
      if (display === "BANNER" || shown.current.has(id)) continue;
      shown.current.add(id);
      const description = (
        <NotificationBody
          notification={notification}
          onOpen={() => toast.dismiss(toastId(id))}
        />
      );
      if (display === "TOAST") {
        toast(title ?? "", { id: toastId(id), description });
        acknowledge(id);
      } else {
        standing.current.add(id);
        toast(title ?? "", {
          id: toastId(id),
          description,
          duration: Infinity,
          dismissible: false,
          action: { label: t("acknowledge"), onClick: () => acknowledge(id) },
        });
      }
    }
  }, [notifications, acknowledge, t]);

  return null;
}
