"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { ExportButton } from "@/components/shared/export-button";
import { downloadPollExcel, finishPoll } from "@/lib/rs/poll";
import { toast } from "@/lib/toast";
import type { PollDetail } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const POLL_LIST_QUERY_KEY = ["poll"] as const;

/**
 * Beside the heading of a stored poll: the way to its answer page, the Excel export of all answers and the
 * button finishing the poll before its deadline. Only what the user may do is offered (the endpoints refuse the
 * rest anyway): the export needs full access, finishing the right to change the running poll.
 */
export function PollHeaderActions({ poll }: { poll: PollDetail | undefined }) {
  const t = useTranslations();
  const qc = useQueryClient();
  const [confirm, setConfirm] = useState(false);
  const onError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));
  const excel = useMutation({
    mutationFn: (id: number) => downloadPollExcel(id),
    onError,
  });
  const finish = useMutation({
    mutationFn: (id: number) => finishPoll(id),
    onSuccess: () => {
      toast.success(t("poll.finish.done"));
      void qc.invalidateQueries({ queryKey: POLL_LIST_QUERY_KEY });
    },
    onError,
  });
  if (poll?.id == null || poll.template) return null;
  const id = poll.id;
  return (
    <>
      {(poll.attendee || poll.fullAccess) && (
        <Button asChild variant="ghost" size="sm" className="gap-1.5">
          <Link href={`/poll/${id}/answer`}>{t("poll.answer.title")}</Link>
        </Button>
      )}
      {poll.fullAccess && (
        <ExportButton
          label={t("exportAsXls")}
          isPending={excel.isPending}
          onClick={() => excel.mutate(id)}
        />
      )}
      {poll.state === "RUNNING" && poll.writeAccess !== false && (
        <>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            className="gap-1.5"
            disabled={finish.isPending}
            onClick={() => setConfirm(true)}
          >
            <HugeiconsIcon icon={Tick02Icon} size={14} aria-hidden />
            {t("poll.finish._")}
          </Button>
          <ConfirmDialog
            open={confirm}
            onOpenChange={setConfirm}
            title={t("poll.finish._")}
            description={t("poll.finish.confirm")}
            confirmLabel={t("poll.finish._")}
            destructive
            onConfirm={() => {
              setConfirm(false);
              finish.mutate(id);
            }}
          />
        </>
      )}
    </>
  );
}
