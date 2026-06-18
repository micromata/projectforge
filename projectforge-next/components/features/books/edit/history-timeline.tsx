"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { HistoryEntry, HistoryEntryAttr } from "../types";

interface Props {
  entries: HistoryEntry[];
}

function initials(name: string | null): string {
  if (!name) return "?";
  return name
    .split(/[\s,]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

function changedFields(attrs: HistoryEntryAttr[]): string[] {
  return attrs
    .map((a) => a.displayPropertyName ?? a.propertyName ?? "")
    .filter(Boolean);
}

export function HistoryTimeline({ entries }: Props) {
  const t = useTranslations("books.edit.history");
  return (
    <ol className="flex flex-col">
      {entries.map((entry, i) => {
        const last = i === entries.length - 1;
        const fields = changedFields(entry.attributes);
        return (
          <li key={entry.id} className="flex gap-3.5">
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
            <div className={cn("flex-1", !last && "pb-5")}>
              <div className="flex items-baseline gap-1.5">
                <span className="text-sm font-semibold">
                  {entry.modifiedByUser ?? "—"}
                </span>
                <span className="text-xs text-foreground/70">
                  {entry.operation}
                </span>
                <span className="ml-auto whitespace-nowrap text-[11px] text-muted-foreground">
                  {entry.timeAgo}
                </span>
              </div>
              {fields.length > 0 && (
                <div className="mt-1.5 rounded-md border border-border/60 bg-muted/40 px-3 py-1.5 text-xs leading-relaxed text-foreground/70">
                  {t("fieldsChanged", { fields: fields.join(", ") })}
                </div>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
