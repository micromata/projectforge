"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { AppNotification } from "@/lib/rs/notification";
import { NotificationActions } from "./notification-actions";
import { NotificationBody } from "./notification-body";
import { NotificationCloseButton } from "./notification-close-button";
import { SEVERITY_CLASSES } from "./severity";

/**
 * A notification as an entry of the bell and as a toast, so both look the same: title, text and the
 * actions below (the link among them), marked by the colour of its severity.
 */
export function NotificationCard({
  notification,
  withActions = true,
  archived = false,
  onOpen,
  onClose,
  className,
}: {
  notification: AppNotification;
  /** False for a toast that counts as confirmed by being shown (`TOAST`): "Open" only. */
  withActions?: boolean;
  /** No longer visible in the app (confirmed, done, …): muted, with its status and date, "Open" only. */
  archived?: boolean;
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
        archived && "opacity-70",
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
      <NotificationBody notification={notification} />
      {archived && <ArchivedState notification={notification} />}
      <NotificationActions
        notification={notification}
        confirmable={withActions && !archived}
        onOpen={onOpen}
      />
    </div>
  );
}

/** The status of an archived notification and when it got it, e.g. "Confirmed 26.10.2026 09:14". */
function ArchivedState({ notification }: { notification: AppNotification }) {
  const t = useTranslations("notification");
  const ctx = useFormatContext();
  const at =
    notification.doneAt ??
    notification.resolvedAt ??
    notification.acknowledgedAt;
  return (
    <div className="text-xs text-muted-foreground">
      {t(`statuses.${notification.status}`)}
      {at && ` ${formatTimestampMinutes(at, ctx)}`}
    </div>
  );
}
