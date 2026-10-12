"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Notification03Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import { useNotificationStore } from "@/store/notification-store";
import { NotificationActions } from "./notification-actions";
import { NotificationBody } from "./notification-body";
import { NotificationCloseButton } from "./notification-close-button";
import { bySeverity, SEVERITY_CLASSES } from "./severity";
import { useMyNotifications } from "./use-my-notifications";

/**
 * The notifications displayed as banners, under the navigation of every page (beside the
 * SystemAlertBanner). A banner stays until it is done: resolved by its rule (e.g. the missing time
 * sheets are booked) or, if the rule allows it, marked as done by the recipient. Closed, it's hidden in
 * this tab until the next reload, but still listed in the bell.
 */
export function NotificationBanners() {
  const { closedIds, closeNotification } = useNotificationStore();
  const banners = bySeverity(
    useMyNotifications().filter(
      (notification) =>
        notification.display === "BANNER" &&
        !closedIds.includes(notification.id)
    )
  );
  if (banners.length === 0) return null;
  return (
    // shrink-0: PageShell is a flex column of fixed height (see SystemAlertBanner).
    <div className="flex shrink-0 flex-col">
      {banners.map((notification) => (
        <div
          key={notification.id}
          role={notification.severity === "URGENT" ? "alert" : "status"}
          data-testid="notification-banner"
          className={cn(
            "relative flex flex-col gap-2 border-b py-2 pr-11 pl-4 text-sm sm:flex-row sm:items-start sm:pr-2",
            SEVERITY_CLASSES[notification.severity]
          )}
        >
          <HugeiconsIcon
            icon={Notification03Icon}
            size={18}
            aria-hidden
            className="mt-px hidden shrink-0 sm:block"
          />
          <div className="min-w-0 flex-1 break-words">
            {notification.title && (
              <div className="font-medium">{notification.title}</div>
            )}
            <NotificationBody notification={notification} />
          </div>
          <NotificationActions notification={notification} />
          <NotificationCloseButton
            onClose={() => closeNotification(notification.id)}
            // Top right on mobile, where the banner is a column.
            className="absolute top-1.5 right-2 sm:static"
          />
        </div>
      ))}
    </div>
  );
}
