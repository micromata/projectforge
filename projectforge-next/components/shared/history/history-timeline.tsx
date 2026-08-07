"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { HistoryEntry } from "@/lib/rs/history";
import { HistoryEntryItem } from "./history-entry-item";
import { HistoryCommentDialog } from "./history-comment-dialog";

export interface HistoryTimelineProps {
  entries: HistoryEntry[];
  /**
   * Whether comments may be appended (`HistoryInfo.supportsUserComments`). Only entities
   * implementing `HistoryUserCommentSupport` — e.g. user, group — allow it.
   */
  supportsUserComments: boolean;
  /** Entity the entries belong to, e.g. "book" — the comment dialog re-reads its history. */
  entity: string;
  /** Id of that entity. */
  entityId: number;
}

/** The change history of one entity, newest first as the backend sorted it. */
export function HistoryTimeline({
  entries,
  supportsUserComments,
  entity,
  entityId,
}: HistoryTimelineProps) {
  const t = useTranslations();
  const [commentOn, setCommentOn] = useState<HistoryEntry | null>(null);

  if (entries.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("nothingFound")}</p>;
  }

  return (
    <>
      <ol className="flex flex-col">
        {entries.map((entry, i) => (
          <HistoryEntryItem
            key={entry.id}
            entry={entry}
            last={i === entries.length - 1}
            onComment={
              supportsUserComments ? () => setCommentOn(entry) : undefined
            }
          />
        ))}
      </ol>
      {commentOn && (
        <HistoryCommentDialog
          entry={commentOn}
          entity={entity}
          entityId={entityId}
          onClose={() => setCommentOn(null)}
        />
      )}
    </>
  );
}
