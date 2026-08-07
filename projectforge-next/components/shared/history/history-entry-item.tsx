"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CommentAdd01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { EntityOpType, HistoryEntry } from "@/lib/rs/history";
import { HistoryAttrDiff } from "./history-attr-diff";

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

/** CSS var holding the colour of an operation, see globals.css. */
function opColor(type: EntityOpType): string {
  if (type === "Insert") return "var(--history-insert)";
  if (type === "Delete") return "var(--history-delete)";
  return "var(--history-update)";
}

/** One change: who changed what, when, and the comments left on it. */
export function HistoryEntryItem({
  entry,
  last,
  onComment,
}: HistoryEntryItemProps) {
  const t = useTranslations();
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
      <div className={cn("min-w-0 flex-1", !last && "pb-5")}>
        <div className="flex flex-wrap items-baseline gap-x-1.5 gap-y-1">
          <span className="text-sm font-semibold">
            {entry.modifiedByUser ?? "—"}
          </span>
          <span className="text-xs text-foreground/70">{entry.operation}</span>
          {entry.diffSummary.map((diff) => (
            <span
              key={diff.type}
              className="text-[11px] font-medium"
              style={{ color: opColor(diff.type) }}
            >
              {diff.count} {diff.operation}
            </span>
          ))}
          <span
            className="ml-auto whitespace-nowrap text-[11px] text-muted-foreground"
            title={entry.modifiedAt}
          >
            {entry.timeAgo}
          </span>
        </div>

        {entry.attributes.length > 0 && (
          <dl className="mt-1.5 flex flex-col gap-1.5 rounded-md border border-border/60 bg-muted/40 px-3 py-2">
            {entry.attributes.map((attr, i) => (
              <HistoryAttrDiff key={attr.id ?? i} attr={attr} />
            ))}
          </dl>
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
      </div>
    </li>
  );
}
