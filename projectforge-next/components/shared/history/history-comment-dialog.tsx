"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { useAppendHistoryComment } from "@/hooks/use-history";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import type { HistoryEntry } from "@/lib/rs/history";

export interface HistoryCommentDialogProps {
  entry: HistoryEntry;
  entity: string;
  entityId: number;
  onClose: () => void;
}

/**
 * Appends a comment to one history entry.
 *
 * Comments are append-only: the existing ones are shown read-only, the new text becomes another
 * timestamped line (see HistoryService.appendUserComment).
 */
export function HistoryCommentDialog({
  entry,
  entity,
  entityId,
  onClose,
}: HistoryCommentDialogProps) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  const [comment, setComment] = useState("");
  const append = useAppendHistoryComment(entity, entityId);

  const submit = async () => {
    try {
      await append.mutateAsync({ entryId: entry.id, comment: comment.trim() });
      onClose();
    } catch {
      toast.error(t("validation.error.generic"));
    }
  };

  // The comment is a textarea, so here it is CTRL-Return that appends — Return stays the line break.
  const canSubmit = comment.trim().length > 0 && !append.isPending;
  const onKeyDown = useSubmitShortcut(() => void submit(), canSubmit);

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent onKeyDown={onKeyDown}>
        <DialogHeader>
          <DialogTitle>{t("history.entry")}</DialogTitle>
          <DialogDescription>
            {entry.modifiedByUser ?? "—"} · {entry.timeAgo}
          </DialogDescription>
        </DialogHeader>

        {entry.userComment && (
          <p className="max-h-40 overflow-y-auto whitespace-pre-wrap rounded-md border border-border bg-muted/40 px-3 py-2 text-xs leading-relaxed">
            {entry.userComment}
          </p>
        )}

        <div className="flex flex-col gap-2">
          <Label htmlFor="history-append-comment">
            {t("history.userComment.append")}
          </Label>
          <Textarea
            id="history-append-comment"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={4}
            autoFocus
          />
          <p className="text-xs text-muted-foreground">
            {t("history.userComment.info")}
          </p>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {t("cancel")}
          </Button>
          <HintTooltip {...shortcutHint}>
            <Button
              type="button"
              onClick={() => void submit()}
              disabled={!canSubmit}
            >
              {append.isPending && <Spinner className="h-4 w-4 border-2" />}
              {t("save")}
            </Button>
          </HintTooltip>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
