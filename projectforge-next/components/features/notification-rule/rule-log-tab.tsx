"use client";

import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  fetchNotificationLog,
  finishPendingNotifications,
} from "@/lib/rs/notification";
import { toast } from "@/lib/toast";
import {
  NotificationLogTable,
  useNotificationLogInvalidation,
} from "./notification-log-table";

/**
 * The notifications the rule has created (at most the last 500): the audit log of the rule. Its editors
 * may end all pending ones at once.
 */
export function RuleLogTab({ id }: { id: number }) {
  const t = useTranslations("notification");
  const query = useQuery({
    queryKey: ["notificationRule", "log", id],
    queryFn: ({ signal }) => fetchNotificationLog(id, signal),
  });
  const [confirm, setConfirm] = useState(false);
  const invalidate = useNotificationLogInvalidation();
  const finishPending = useMutation({
    mutationFn: () => finishPendingNotifications(id),
    onSuccess: (result) => {
      toast.success(t("finished", { arg0: result.finished }));
      invalidate();
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const anyFinishable = query.data?.some((r) => r.finishable) === true;
  return (
    <>
      {anyFinishable && (
        <div className="flex justify-end px-4 pt-4">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={finishPending.isPending}
            onClick={() => setConfirm(true)}
          >
            {t("finishPending")}
          </Button>
        </div>
      )}
      <NotificationLogTable
        rows={query.data}
        isLoading={query.isLoading}
        emptyText={t("logEmpty")}
      />
      <ConfirmDialog
        open={confirm}
        onOpenChange={setConfirm}
        title={t("finishPending")}
        description={t("finishPendingConfirm")}
        confirmLabel={t("finishPending")}
        destructive
        onConfirm={() => finishPending.mutate()}
      />
    </>
  );
}
