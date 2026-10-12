"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchNotificationLog } from "@/lib/rs/notification";
import { NotificationLogTable } from "./notification-log-table";

/** The notifications the rule has created (at most the last 500): the audit log of the rule. */
export function RuleLogTab({ id }: { id: number }) {
  const t = useTranslations("notification");
  const query = useQuery({
    queryKey: ["notificationRule", "log", id],
    queryFn: ({ signal }) => fetchNotificationLog(id, signal),
  });
  return (
    <NotificationLogTable
      rows={query.data}
      isLoading={query.isLoading}
      emptyText={t("logEmpty")}
    />
  );
}
