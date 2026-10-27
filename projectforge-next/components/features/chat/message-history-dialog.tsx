"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HistoryTimeline } from "@/components/shared/history/history-timeline";
import { Spinner } from "@/components/shared/spinner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { fetchChatMessageHistory } from "@/lib/rs/chat";

/** The edits, the deletion and the hiding of a message, for moderators (MSG-03, SEC-05). */
export function MessageHistoryDialog({
  messageId,
  onClose,
}: {
  messageId: number | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["chatMessageHistory", messageId],
    queryFn: () => fetchChatMessageHistory(messageId!),
    enabled: messageId != null,
  });
  return (
    <Dialog
      open={messageId != null}
      onOpenChange={(open) => !open && onClose()}
    >
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("chat.message.history")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("chat.message.history")}
          </DialogDescription>
        </DialogHeader>
        {query.isPending && <Spinner className="h-5 w-5 border-2" />}
        {query.data && messageId != null && (
          <HistoryTimeline
            entries={query.data}
            supportsUserComments={false}
            entity="chatMessage"
            entityId={messageId}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
