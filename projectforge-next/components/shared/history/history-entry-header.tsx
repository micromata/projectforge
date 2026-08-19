"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { CollapsibleTrigger } from "@/components/ui/collapsible";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { HistoryEntry } from "@/lib/rs/history";
import { opColor } from "./history-op-style";

export interface HistoryEntryHeaderProps {
  entry: HistoryEntry;
  open: boolean;
}

/** The changed fields of an entry as one line, as the legacy row shows them. */
function fieldList(entry: HistoryEntry): string {
  return entry.attributes
    .map((attr) => attr.displayPropertyName ?? attr.propertyName)
    .filter(Boolean)
    .join(", ");
}

/**
 * The always visible line of one history entry: who, how many changes of which kind, which fields,
 * and how long ago. Clicking it reveals the values.
 *
 * The fields are named only while the entry is folded ([CollapsibleSummary]) — open, the diff below
 * lists every one of them with its values, so the line would be the same enumeration twice.
 *
 * The relative time sits *beside* the trigger, not inside it: it carries a tooltip with the exact
 * timestamp, and a tooltip trigger nested in a button would swallow the click.
 */
export function HistoryEntryHeader({ entry, open }: HistoryEntryHeaderProps) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const fields = fieldList(entry);

  return (
    <div className="flex items-start gap-2">
      <CollapsibleTrigger className="min-w-0 flex-1 cursor-pointer text-left">
        <CollapsibleSummary
          // Wrapped rather than truncated: user and change counts are all short, and none of them is
          // the part that may be cut.
          primaryClassName="flex-wrap items-baseline gap-x-1.5 gap-y-1"
          primary={
            <>
              <HugeiconsIcon
                icon={ArrowRight01Icon}
                size={14}
                aria-hidden
                className={cn(
                  "shrink-0 self-center text-muted-foreground transition-transform",
                  open && "rotate-90"
                )}
              />
              <span className="text-sm font-semibold">
                {entry.modifiedByUser ?? "—"}
              </span>
              {entry.diffSummary.map((diff) => (
                <span
                  key={diff.type}
                  className="shrink-0 text-[11px] font-medium"
                  style={{ color: opColor(diff.type) }}
                >
                  {diff.count} {diff.operation}
                </span>
              ))}
            </>
          }
          details={[
            fields && (
              <span className="block truncate">
                {t("history.fields")}: {fields}
              </span>
            ),
          ]}
        />
      </CollapsibleTrigger>
      <HintTooltip
        plain
        side="left"
        text={formatTimestampMinutes(entry.modifiedAt, ctx)}
      >
        <span className="mt-0.5 shrink-0 whitespace-nowrap text-[11px] text-muted-foreground">
          {entry.timeAgo}
        </span>
      </HintTooltip>
    </div>
  );
}
