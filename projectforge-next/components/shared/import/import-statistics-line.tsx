"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { statisticsEntries } from "./import-model";
import type { ImportStorageInfo } from "./import-types";

interface Props {
  info?: ImportStorageInfo;
  /**
   * The status-group keys currently hidden (see STATUS_GROUP_OF). When passed together with
   * [onToggleKey], each per-status count becomes a toggle chip; without them the line stays plain text.
   */
  hiddenKeys?: Set<string>;
  onToggleKey?: (key: string) => void;
}

/** The tint each toned count reads in — the same palette as the row it counts (see rowClassForStatus). */
const TONE_CLASS: Record<string, string> = {
  new: "text-brand-green-dark",
  modified: "text-brand-teal",
  deleted: "text-brand-pink",
  faulty: "text-destructive",
  unknown: "text-brand-pink",
};

/**
 * The one-line summary of an upload: the total, then each non-zero per-status count in its tint. Built by
 * [statisticsEntries] so a clean import shows just the total rather than a row of zeroes. The detected and
 * unknown columns are the reference at the foot of the preview (see ImportColumnInfo), not part of this line.
 */
export function ImportStatisticsLine({ info, hiddenKeys, onToggleKey }: Props) {
  const t = useTranslations();
  if (!info) return null;
  const stats = statisticsEntries(info);
  const interactive = Boolean(onToggleKey);

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
      {stats.map((stat) => {
        const label = t(stat.labelKey);
        const count = (
          <>
            <span className="text-muted-foreground">{label}:</span>
            <span
              className={cn(
                "font-semibold tabular-nums",
                stat.tone && TONE_CLASS[stat.tone]
              )}
            >
              {stat.count}
            </span>
          </>
        );
        // The total is never a filter; only the per-status counts toggle their rows.
        if (!interactive || stat.key === "total") {
          return (
            <span key={stat.key} className="flex items-center gap-1">
              {count}
            </span>
          );
        }
        const hidden = hiddenKeys?.has(stat.key) ?? false;
        return (
          <button
            key={stat.key}
            type="button"
            aria-pressed={!hidden}
            aria-label={label}
            onClick={() => onToggleKey?.(stat.key)}
            className={cn(
              "flex items-center gap-1 rounded-md px-1.5 py-0.5 transition-colors",
              "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
              hidden && "opacity-40"
            )}
          >
            {count}
          </button>
        );
      })}
    </div>
  );
}
