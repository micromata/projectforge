"use client";

import { RichText } from "@/components/shared/rich-text";
import type { AppNotification } from "@/lib/rs/notification";

/** The text of a notification (its link is the "Open" button of [NotificationActions]). */
export function NotificationBody({
  notification,
}: {
  notification: AppNotification;
}) {
  return (
    <div className="space-y-1 text-sm">
      {notification.body && <RichText html={notification.body} />}
    </div>
  );
}
