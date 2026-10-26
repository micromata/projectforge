"use client";

import { useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import {
  finishNotification,
  type NotificationLogEntry,
} from "@/lib/rs/notification";
import { toast } from "@/lib/toast";
import { DEACTIVATED_REF_CLASS } from "@/lib/page-def/reference";
import { cn } from "@/lib/utils";

type Column = ColumnDef<NotificationLogEntry, unknown>;
type Step = NotificationLogEntry["deliverySteps"][number];

/**
 * Notifications as an audit log: who got which, when it was confirmed and by whom, and how far its
 * delivery cascade got. Shared by the log tab of a rule and the log of all rules (`withRule` adds the
 * rule's column there). Editors of a rule may end its pending notifications here (e.g. a banner nobody
 * can mark as done).
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
  const finish = useFinishNotification();
  const [confirmFinish, setConfirmFinish] = useState<number | null>(null);
  const anyFinishable = rows?.some((r) => r.finishable) === true;
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
      {
        ...column("recipient", t("recipient"), 160, (r) => r.recipient ?? ""),
        // Struck through when the recipient is no longer active (see inactiveRefClass).
        cell: ({ row }) => (
          <span
            className={cn(
              row.original.recipientDeactivated && DEACTIVATED_REF_CLASS
            )}
          >
            {row.original.recipient}
          </span>
        ),
      },
      column("status", t("status"), 110, (r) => t(`statuses.${r.status}`)),
      column("periodKey", t("preview.period"), 100, (r) => r.periodKey ?? ""),
      column("created", t("created"), 130, (r) => time(r.created)),
      {
        ...column("acknowledgedAt", t("acknowledgedAt"), 180, (r) =>
          [time(r.acknowledgedAt), r.acknowledgedBy].filter(Boolean).join(", ")
        ),
        cell: ({ row }) => {
          const r = row.original;
          return (
            <>
              {time(r.acknowledgedAt)}
              {r.acknowledgedAt && r.acknowledgedBy && ", "}
              <span
                className={cn(
                  r.acknowledgedByDeactivated && DEACTIVATED_REF_CLASS
                )}
              >
                {r.acknowledgedBy}
              </span>
            </>
          );
        },
      },
      column("finished", t("resolvedAt"), 130, (r) =>
        time(r.doneAt ?? r.resolvedAt)
      ),
      column("delivery", t("deliverySteps"), 320, (r) =>
        r.deliverySteps.map(stepState).join("; ")
      ),
      ...(anyFinishable
        ? [
            {
              id: "finish",
              header: "",
              size: 100,
              enableSorting: false,
              cell: ({ row }) =>
                row.original.finishable ? (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setConfirmFinish(row.original.id)}
                  >
                    {t("finish")}
                  </Button>
                ) : null,
            } satisfies Column,
          ]
        : []),
    ];
  }, [t, ctx, withRule, anyFinishable]);

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
      <ConfirmDialog
        open={confirmFinish != null}
        onOpenChange={(open) => !open && setConfirmFinish(null)}
        title={t("finish")}
        description={t("finishConfirm")}
        confirmLabel={t("finish")}
        destructive
        onConfirm={() => confirmFinish != null && finish.mutate(confirmFinish)}
      />
    </div>
  );
}

/** Reloads the logs and the own notifications (e.g. an own banner just ended). */
export function useNotificationLogInvalidation() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({
      queryKey: ["notificationRule", "log"],
    });
    void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
    void queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };
}

function useFinishNotification() {
  const t = useTranslations("notification");
  const invalidate = useNotificationLogInvalidation();
  return useMutation({
    mutationFn: finishNotification,
    onSuccess: () => {
      toast.success(t("finished", { arg0: 1 }));
      invalidate();
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
}
