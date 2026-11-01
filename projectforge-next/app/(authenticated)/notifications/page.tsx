"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { NotificationCard } from "@/components/shared/notifications/notification-card";
import { ArchivedSection } from "@/components/shared/notifications/notification-bell";
import { bySeverity } from "@/components/shared/notifications/severity";
import { useMyNotifications } from "@/components/shared/notifications/use-my-notifications";

/**
 * The notification center as a page (`/next/notifications`): what the bell's menu shows. The bell links here in the
 * mobile app, which opens its own list for this url (see NativePage in the app); with its setting for the web
 * version, this page is shown.
 */
export default function NotificationsRoute() {
  const t = useTranslations("notification");
  const notifications = bySeverity(useMyNotifications());
  return (
    <PageShell>
      <PageTitleRow title={t("center")} />
      <div className="space-y-1 px-4">
        {notifications.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("centerEmpty")}</p>
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
    </PageShell>
  );
}
