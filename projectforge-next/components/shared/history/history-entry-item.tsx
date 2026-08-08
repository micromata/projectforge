"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CommentAdd01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";
import type { HistoryEntry } from "@/lib/rs/history";
import { HistoryAttrDiff } from "./history-attr-diff";
import { HistoryEntryHeader } from "./history-entry-header";

export interface HistoryEntryItemProps {
  entry: HistoryEntry;
  /** The last entry has no connector line below its avatar. */
  last: boolean;
  /** Opens the comment dialog; absent when the entity supports no comments. */
  onComment?: () => void;
}

/** Two initials of a name like "Fink, Laura", the avatar's content. */
function initials(name: string | null): string {
  if (!name) return "?";
  return name
    .split(/[\s,]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * One change: who changed what, when, and the comments left on it.
 *
 * Collapsed by default, like the legacy row: an insert brings one attribute per property, so an
 * always expanded list turns a long history into an unreadable page.
 */
export function HistoryEntryItem({
  entry,
  last,
  onComment,
}: HistoryEntryItemProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  return (
    <li className="flex gap-3.5">
      <div className="flex shrink-0 flex-col items-center">
        <div
          aria-hidden
          className="flex size-7 items-center justify-center rounded-full border-[1.5px] text-[11px] font-bold"
          style={{
            background: "var(--status-available-bg)",
            borderColor: "var(--status-available-border)",
            color: "var(--primary)",
          }}
        >
          {initials(entry.modifiedByUser)}
        </div>
        {!last && <div className="my-1 w-px flex-1 bg-border" />}
      </div>
      <Collapsible
        open={open}
        onOpenChange={setOpen}
        className={cn("min-w-0 flex-1", !last && "pb-5")}
      >
        <HistoryEntryHeader entry={entry} open={open} />

        <CollapsibleContent>
          {entry.attributes.length > 0 && (
            <>
              <h4 className="mt-2 text-xs font-semibold">{t("changes")}</h4>
              <dl className="mt-1 flex flex-col gap-1.5 rounded-md border border-border/60 bg-muted/40 px-3 py-2">
                {entry.attributes.map((attr, i) => (
                  <HistoryAttrDiff key={attr.id ?? i} attr={attr} />
                ))}
              </dl>
            </>
          )}

          {entry.userComment && (
            <p
              className="mt-1.5 whitespace-pre-wrap rounded-md border px-3 py-2 text-xs leading-relaxed"
              style={{
                background: "var(--history-comment-bg)",
                borderColor: "var(--history-comment-border)",
              }}
            >
              {entry.userComment}
            </p>
          )}

          {onComment && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="mt-1 h-7 px-2 text-xs"
              onClick={onComment}
            >
              <HugeiconsIcon icon={CommentAdd01Icon} size={14} />
              {t("history.userComment.append")}
            </Button>
          )}
        </CollapsibleContent>
      </Collapsible>
    </li>
  );
}
