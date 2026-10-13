"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { AppNotification } from "@/lib/rs/notification";
import { useNotificationActions } from "./use-my-notifications";

/**
 * Confirm (an open notification: no further escalation) and mark as done (a banner whose rule allows
 * it; otherwise a banner goes away once its cause is resolved).
 */
export function NotificationActions({
  notification,
  className,
}: {
  notification: AppNotification;
  className?: string;
}) {
  const t = useTranslations("notification");
  const { acknowledge, done, isPending } = useNotificationActions();
  const canDone = notification.display === "BANNER" && notification.manualDone;
  const canAcknowledge = notification.status === "OPEN";
  if (!canDone && !canAcknowledge) return null;
  // Solid, so the buttons stay readable on every severity colour (white text on red for URGENT).
  const buttonClass = "bg-background text-foreground hover:bg-muted";
  return (
    <div className={cn("flex shrink-0 gap-2", className)}>
      {canAcknowledge && (
        <Button
          size="sm"
          variant="outline"
          className={buttonClass}
          disabled={isPending}
          onClick={() => acknowledge(notification.id)}
        >
          {t("acknowledge")}
        </Button>
      )}
      {canDone && (
        <Button
          size="sm"
          variant="outline"
          className={buttonClass}
          disabled={isPending}
          onClick={() => done(notification.id)}
        >
          {t("markDone")}
        </Button>
      )}
    </div>
  );
}
