"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Notification03Icon } from "@hugeicons/core-free-icons";
import { buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarLabel,
  MenubarMenu,
  MenubarSeparator,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { cn } from "@/lib/utils";
import { NotificationActions } from "./notification-actions";
import { NotificationBody } from "./notification-body";
import { bySeverity, SEVERITY_CLASSES } from "./severity";
import { useMyNotifications } from "./use-my-notifications";

/**
 * The bell of the top navigation: the counter of the open notifications and, opened, all of them with
 * their actions — a toast to be confirmed can be confirmed here as well, a banner marked as done.
 * Belongs inside the nav's `Menubar`.
 */
export function NotificationBell() {
  const t = useTranslations("notification");
  const notifications = bySeverity(useMyNotifications());
  return (
    <MenubarMenu>
      <MenubarTrigger
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "relative shrink-0 cursor-pointer"
        )}
        aria-label={t("center")}
        data-testid="notification-bell"
      >
        <HugeiconsIcon icon={Notification03Icon} size={18} />
        <MenuCounterBadge
          badge={{ counter: notifications.length }}
          variant="corner"
        />
      </MenubarTrigger>
      <MenubarContent align="end" className="w-80 max-w-[calc(100vw-1rem)]">
        <MenubarLabel>{t("center")}</MenubarLabel>
        <MenubarSeparator />
        {notifications.length === 0 ? (
          <p className="px-2 py-1.5 text-sm text-muted-foreground">
            {t("centerEmpty")}
          </p>
        ) : (
          <div className="max-h-[70vh] space-y-1 overflow-y-auto">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={cn(
                  "space-y-2 rounded-sm border-l-4 px-2 py-1.5",
                  SEVERITY_CLASSES[notification.severity],
                  // The urgent colour is too loud for a list; its border marks it.
                  notification.severity === "URGENT" &&
                    "bg-transparent text-foreground"
                )}
              >
                {notification.title && (
                  <div className="text-sm font-medium">
                    {notification.title}
                  </div>
                )}
                <NotificationBody notification={notification} />
                <NotificationActions notification={notification} />
              </div>
            ))}
          </div>
        )}
      </MenubarContent>
    </MenubarMenu>
  );
}
