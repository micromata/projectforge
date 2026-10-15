"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { LegendEntry } from "@/lib/page-def/types";

interface TableLegendProps {
  entries: LegendEntry[];
  className?: string;
}

/**
 * Colour legend rendered below a list table.
 *
 * Each entry shows a small colour swatch (matching the row-* token) and its translated label.
 * The `row-deleted` entry additionally shows a struck-through sample text, mirroring what that
 * class does to real rows.
 */
export function TableLegend({ entries, className }: TableLegendProps) {
  const t = useTranslations();

  if (entries.length === 0) return null;

  return (
    <div
      className={cn(
        "flex flex-wrap gap-x-4 gap-y-1 border-t px-2 py-1.5 text-[11px] text-muted-foreground",
        className
      )}
    >
      {entries.map((entry) => (
        <span key={entry.className} className="flex items-center gap-1.5">
          <span
            className={cn(
              "inline-block h-3 w-3 rounded-sm border",
              entry.className
            )}
            aria-hidden
          />
          <span className={cn(entry.strikethrough && "line-through")}>
            {/* A backend key may be a namespace as well as a text, and the pages name the bare one. */}
            {t(leafKeyOf(entry.labelKey, t.has))}
          </span>
        </span>
      ))}
    </div>
  );
}
