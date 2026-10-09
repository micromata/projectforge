"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { PersonSelect } from "@/components/shared/person-select";
import { PageTitleRow } from "@/components/shared/page-title-row";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  fetchAllNotificationLog,
  type NotificationStatus,
} from "@/lib/rs/notification";
import { NotificationLogTable } from "./notification-log-table";

const STATUSES: NotificationStatus[] = [
  "OPEN",
  "ACKNOWLEDGED",
  "DONE",
  "RESOLVED",
  "EXPIRED",
];

/** Radix Select allows no empty value, so "all statuses" is a value of its own. */
const ALL = "ALL";

/**
 * The log of the notifications of all rules (`/notificationRule/log`), reached from the rule list: who
 * got which and who confirmed it, when, filterable by recipient and status. Admins and finance only,
 * enforced by the endpoint (`NotificationRuleEntityRest.getLog`).
 */
export function NotificationLogPage() {
  const t = useTranslations();
  const [recipient, setRecipient] = useState<EntityRef | null>(null);
  const [status, setStatus] = useState<NotificationStatus | null>(null);
  const filter = { recipientId: recipient?.id ?? null, status };
  const query = useQuery({
    queryKey: ["notificationRule", "log", "all", filter],
    queryFn: ({ signal }) => fetchAllNotificationLog(filter, signal),
    placeholderData: keepPreviousData,
  });
  const filtered = filter.recipientId != null || status != null;

  return (
    <>
      <PageTitleRow
        category={t("notification.rule.title")}
        title={t("notification.log")}
      />
      <div className="flex flex-wrap items-end gap-4 px-4 pt-3">
        <div className="flex flex-col gap-1">
          <Label htmlFor="notification-log-recipient">
            {t("notification.recipient")}
          </Label>
          <PersonSelect
            id="notification-log-recipient"
            value={recipient}
            onChange={setRecipient}
            className="w-64"
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="notification-log-status">
            {t("notification.status")}
          </Label>
          <Select
            value={status ?? ALL}
            onValueChange={(value) =>
              setStatus(value === ALL ? null : (value as NotificationStatus))
            }
          >
            <SelectTrigger id="notification-log-status" className="h-9 w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{t("filter.all")}</SelectItem>
              {STATUSES.map((value) => (
                <SelectItem key={value} value={value}>
                  {t(`notification.statuses.${value}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      <NotificationLogTable
        rows={query.data}
        isLoading={query.isLoading}
        withRule
        emptyText={t(
          filtered ? "notification.logEmptyFilter" : "notification.logEmpty"
        )}
      />
    </>
  );
}
