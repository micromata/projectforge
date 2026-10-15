"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber, formatTimestampMinutes } from "@/lib/format";
import type { LogGroupDetail } from "@/lib/rs/admin-errors";
import { AUDIENCE_KEYS, NOTIFY_KEYS } from "./admin-errors-labels";

/** What is known about a problem at a glance: where, since when, how often, and who gets told. */
export function AdminErrorFacts({ detail }: { detail: LogGroupDetail }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const entry = detail.entry;
  const timestamp = (value?: number | null) =>
    value != null ? formatTimestampMinutes(value, ctx) : null;
  const count = (value: number) => formatNumber(value, ctx, 0);
  return (
    <dl className="grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
      <Fact label={t("system.admin.adminErrors.code")} mono>
        {entry.code}
      </Fact>
      <Fact label={t("system.admin.adminErrors.location")} mono>
        {entry.location}
      </Fact>
      <Fact label={t("system.admin.adminErrors.exceptionClass")} mono>
        {entry.exceptionClass}
      </Fact>
      <Fact label={t("system.admin.adminErrors.firstSeen")}>
        {timestamp(entry.firstSeen)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.lastSeen")}>
        {timestamp(entry.lastSeen)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.lastNotified")}>
        {timestamp(detail.lastNotified)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.totalCount")}>
        {count(entry.totalCount)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.kpi.occurrences")}>
        {count(entry.count24h)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.distinctUsers24h")}>
        {count(detail.distinctUsers24h)}
      </Fact>
      <Fact label={t("system.admin.adminErrors.notify._")}>
        {t(NOTIFY_KEYS[entry.notify])}
      </Fact>
      <Fact label={t("system.admin.adminErrors.audience._")}>
        {t(AUDIENCE_KEYS[detail.audience])}
      </Fact>
      <Fact label={t("system.admin.adminErrors.threshold")}>
        {count(detail.threshold)}
      </Fact>
      {detail.reopenedAt != null && (
        <Fact label={t("system.admin.adminErrors.reopenedAt")}>
          {timestamp(detail.reopenedAt)}
        </Fact>
      )}
    </dl>
  );
}

export function Fact({
  label,
  mono,
  children,
}: {
  label: string;
  mono?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-w-0 gap-2">
      <dt className="shrink-0 text-muted-foreground">{label}:</dt>
      <dd
        className={mono ? "truncate font-mono text-xs leading-5" : "truncate"}
      >
        {children || "–"}
      </dd>
    </div>
  );
}

/** A labelled block of text: the explanation, the sample message, the stack trace … */
export function DetailText({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-0.5">
      <div className="text-xs font-medium text-muted-foreground">{label}</div>
      <div className="text-sm">{children}</div>
    </div>
  );
}
