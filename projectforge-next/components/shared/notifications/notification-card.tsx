"use client";

import { cn } from "@/lib/utils";
import type { AppNotification } from "@/lib/rs/notification";
import { NotificationActions } from "./notification-actions";
import { NotificationBody } from "./notification-body";
import { NotificationCloseButton } from "./notification-close-button";
import { SEVERITY_CLASSES } from "./severity";

/**
 * A notification as an entry of the bell and as a toast, so both look the same: title, text, link and
 * the actions below, marked by the colour of its severity.
 */
export function NotificationCard({
  notification,
  withActions = true,
  onOpen,
  onClose,
  className,
}: {
  notification: AppNotification;
  /** False for a toast that counts as confirmed by being shown (`TOAST`). */
  withActions?: boolean;
  /** Called when the link is followed, e.g. to close the toast or menu it is shown in. */
  onOpen?: () => void;
  /** Shows a close button (a toast; the bell has no need of it). */
  onClose?: () => void;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "space-y-2 rounded-sm border-l-4 px-2 py-1.5",
        SEVERITY_CLASSES[notification.severity],
        // The urgent colour is too loud for a card; its border marks it.
        notification.severity === "URGENT" && "bg-transparent text-foreground",
        className
      )}
    >
      {(notification.title || onClose) && (
        <div className="flex items-start gap-2">
          <div className="min-w-0 flex-1 text-sm font-medium">
            {notification.title}
          </div>
          {onClose && (
            <NotificationCloseButton
              onClose={onClose}
              className="-my-1 -mr-1"
            />
          )}
        </div>
      )}
      <NotificationBody notification={notification} onOpen={onOpen} />
      {withActions && <NotificationActions notification={notification} />}
    </div>
  );
}
