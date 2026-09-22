"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { HistoryEntry } from "@/lib/rs/history";
import { HistoryEntryItem } from "./history-entry-item";
import { HistoryCommentDialog } from "./history-comment-dialog";

/** How many entries are rendered before "show more"; a history can hold thousands. */
const PAGE_SIZE = 50;

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

/**
 * The change history of one entity, newest first as the backend sorted it.
 *
 * The endpoint answers with the whole history, so only a page of it is rendered — a long-lived
 * entity has thousands of entries, and each one mounts a row of its own.
 */
export function HistoryTimeline({
  entries,
  supportsUserComments,
  entity,
  entityId,
}: HistoryTimelineProps) {
  const t = useTranslations();
  const [commentOn, setCommentOn] = useState<HistoryEntry | null>(null);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);

  if (entries.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("nothingFound")}</p>;
  }

  const visible = entries.slice(0, visibleCount);
  const hidden = entries.length - visible.length;

  return (
    <>
      <ol className="flex flex-col">
        {visible.map((entry, i) => (
          <HistoryEntryItem
            key={entry.id}
            entry={entry}
            last={i === visible.length - 1}
            onComment={
              supportsUserComments ? () => setCommentOn(entry) : undefined
            }
          />
        ))}
      </ol>
      {hidden > 0 && (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="mt-1 self-start text-xs"
          onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
        >
          {t("history.showMore")} ({hidden})
        </Button>
      )}
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
