"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { NOTIFICATION_RULE_PAGE } from "@/components/features/notification-rule/notification-rule.page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function NotificationRuleEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/notificationRule/[id]")?.id;
  if (raw === undefined) return null;
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isInteger(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={NOTIFICATION_RULE_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
