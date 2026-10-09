"use client";

import { PageShell } from "@/components/shared/page-shell";
import { NotificationLogPage } from "@/components/features/notification-rule/notification-log-page";

/**
 * The log of the notifications of all rules (`/next/notificationRule/log`), reached from the rule list's
 * toolbar rather than a menu entry. A concrete route beside `[id]`, as `employeeSalary/import`.
 */
export default function NotificationLogRoute() {
  return (
    <PageShell>
      <NotificationLogPage />
    </PageShell>
  );
}
