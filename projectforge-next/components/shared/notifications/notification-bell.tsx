"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Notification03Icon,
} from "@hugeicons/core-free-icons";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarLabel,
  MenubarMenu,
  MenubarSeparator,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { cn } from "@/lib/utils";
import { useNotificationStore } from "@/store/notification-store";
import { NotificationCard } from "./notification-card";
import { bySeverity } from "./severity";
import {
  useMyArchivedNotifications,
  useMyNotifications,
} from "./use-my-notifications";

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
      <MenubarContent
        align="end"
        className="w-[28rem] max-w-[calc(100vw-1rem)]"
      >
        <BellOpenMarker />
        <MenubarLabel>{t("center")}</MenubarLabel>
        <MenubarSeparator />
        <div className="max-h-[70vh] space-y-1 overflow-y-auto">
          {notifications.length === 0 ? (
            <p className="px-2 py-1.5 text-sm text-muted-foreground">
              {t("centerEmpty")}
            </p>
          ) : (
            notifications.map((notification) => (
              <NotificationCard
                key={notification.id}
                notification={notification}
              />
            ))
          )}
          <ArchivedSection />
        </div>
      </MenubarContent>
    </MenubarMenu>
  );
}

/**
 * The button "Archived entries" and, once clicked, the notifications no longer visible (confirmed,
 * done, …), so one confirmed by mistake can still be read. Hidden again when the menu closes (the
 * state is unmounted with it).
 */
function ArchivedSection() {
  const t = useTranslations("notification");
  const [shown, setShown] = useState(false);
  return (
    <div>
      <MenubarSeparator />
      <Button
        variant="ghost"
        size="sm"
        className="w-full justify-start text-muted-foreground"
        aria-expanded={shown}
        onClick={() => setShown((value) => !value)}
        data-testid="notification-archived-toggle"
      >
        <HugeiconsIcon
          icon={shown ? ArrowUp01Icon : ArrowDown01Icon}
          size={16}
        />
        {t("centerArchived")}
      </Button>
      {shown && <ArchivedNotifications />}
    </div>
  );
}

/** Loaded when mounted (the section opened), not polled. */
function ArchivedNotifications() {
  const t = useTranslations("notification");
  const { notifications: archived, isLoading } = useMyArchivedNotifications();
  if (isLoading) return null;
  if (archived.length === 0) {
    return (
      <p className="px-2 py-1.5 text-sm text-muted-foreground">
        {t("centerArchivedEmpty")}
      </p>
    );
  }
  return (
    <div className="space-y-1" data-testid="notification-archived">
      {archived.map((notification) => (
        <NotificationCard
          key={notification.id}
          notification={notification}
          archived
        />
      ))}
    </div>
  );
}

/**
 * Mounted only while the bell's menu is open (Radix's `MenubarMenu` has no `onOpenChange`): tells
 * NotificationToasts, whose toast to be confirmed would otherwise cover the menu top right.
 */
function BellOpenMarker() {
  const setBellOpen = useNotificationStore((s) => s.setBellOpen);
  useEffect(() => {
    setBellOpen(true);
    return () => setBellOpen(false);
  }, [setBellOpen]);
  return null;
}
