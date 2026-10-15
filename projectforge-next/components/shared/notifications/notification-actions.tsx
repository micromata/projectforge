"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { MenuLink } from "@/components/shared/menu-link";
import { cn } from "@/lib/utils";
import type { AppNotification } from "@/lib/rs/notification";
import { useNotificationActions } from "./use-my-notifications";

/**
 * Open (the link to where its cause is resolved, e.g. the monthly report, if the rule gives one), confirm
 * (an open notification: no further escalation) and mark as done (a banner whose rule allows it;
 * otherwise a banner goes away once its cause is resolved).
 */
export function NotificationActions({
  notification,
  confirmable = true,
  onOpen,
  className,
}: {
  notification: AppNotification;
  /** False for a toast that counts as confirmed by being shown (`TOAST`): only "Open" is offered. */
  confirmable?: boolean;
  /** Called when the link is followed, e.g. to close the toast or menu it is shown in. */
  onOpen?: () => void;
  className?: string;
}) {
  const t = useTranslations("notification");
  const { acknowledge, done, isPending } = useNotificationActions();
  const canDone =
    confirmable && notification.display === "BANNER" && notification.manualDone;
  const canAcknowledge = confirmable && notification.status === "OPEN";
  const link = notification.link;
  if (!link && !canDone && !canAcknowledge) return null;
  // Solid, so the buttons stay readable on every severity colour (white text on red for URGENT).
  const buttonClass = "bg-background text-foreground hover:bg-muted";
  return (
    <div className={cn("flex shrink-0 gap-2", className)}>
      {link && (
        <Button size="sm" variant="outline" className={buttonClass} asChild>
          <MenuLink url={link} onClick={onOpen}>
            {t("open")}
          </MenuLink>
        </Button>
      )}
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
