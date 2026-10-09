"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { NotificationLogEntry } from "@/lib/rs/notification";

type Column = ColumnDef<NotificationLogEntry, unknown>;
type Step = NotificationLogEntry["deliverySteps"][number];

/**
 * Notifications as an audit log: who got which, when it was confirmed and by whom, and how far its
 * delivery cascade got. Shared by the log tab of a rule and the log of all rules (`withRule` adds the
 * rule's column there).
 */
export function NotificationLogTable({
  rows,
  isLoading,
  withRule = false,
  emptyText,
}: {
  rows: NotificationLogEntry[] | undefined;
  isLoading: boolean;
  withRule?: boolean;
  emptyText: string;
}) {
  const t = useTranslations("notification");
  const ctx = useFormatContext();
  const columns = useMemo((): Column[] => {
    const time = (value?: string | null) =>
      value ? formatTimestampMinutes(value, ctx) : "";
    const stepState = (step: Step) => {
      const channel = t(`channels.${step.channel}`);
      if (step.skipped) return `${channel}: ${t("stepState.skipped")}`;
      if (step.sentAt) return `${channel}: ${time(step.sentAt)}`;
      if (step.failed) return `${channel}: ${t("stepState.failed")}`;
      if (step.attempts > 0)
        return `${channel}: ${t("stepState.retry", { arg0: step.attempts })}`;
      return `${channel}: ${t("stepState.pending")} (${time(step.dueAt)})`;
    };
    const column = (
      id: string,
      header: string,
      size: number,
      accessorFn: (row: NotificationLogEntry) => string
    ): Column => ({ id, header, size, accessorFn, meta: { label: header } });
    return [
      ...(withRule
        ? [column("rule", t("ruleName"), 160, (r) => r.ruleName ?? "")]
        : []),
      column("recipient", t("recipient"), 160, (r) => r.recipient ?? ""),
      column("status", t("status"), 110, (r) => t(`statuses.${r.status}`)),
      column("periodKey", t("preview.period"), 100, (r) => r.periodKey ?? ""),
      column("created", t("created"), 130, (r) => time(r.created)),
      column("acknowledgedAt", t("acknowledgedAt"), 180, (r) =>
        [time(r.acknowledgedAt), r.acknowledgedBy].filter(Boolean).join(", ")
      ),
      column("finished", t("resolvedAt"), 130, (r) =>
        time(r.doneAt ?? r.resolvedAt)
      ),
      column("delivery", t("deliverySteps"), 320, (r) =>
        r.deliverySteps.map(stepState).join("; ")
      ),
    ];
  }, [t, ctx, withRule]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-4">
        <Spinner className="h-5 w-5 border-2" />
      </div>
    );
  }
  if (!rows || rows.length === 0) {
    return <p className="p-4 text-sm text-muted-foreground">{emptyText}</p>;
  }
  return (
    <div className="flex min-h-0 flex-1 flex-col p-4">
      <DataTable<NotificationLogEntry>
        columns={columns}
        data={rows}
        enableColumnFilters={false}
        manualSorting={false}
        showPagination={false}
        getRowId={(row) => String(row.id)}
      />
    </div>
  );
}
