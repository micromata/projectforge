"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { statisticsEntries } from "./import-model";
import type { ImportStorageInfo } from "./import-types";

interface Props {
  info?: ImportStorageInfo;
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
export function ImportStatisticsLine({ info }: Props) {
  const t = useTranslations();
  if (!info) return null;
  const stats = statisticsEntries(info);

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
      {stats.map((stat) => (
        <span key={stat.key} className="flex items-center gap-1">
          <span className="text-muted-foreground">{t(stat.labelKey)}:</span>
          <span
            className={cn(
              "font-semibold tabular-nums",
              stat.tone && TONE_CLASS[stat.tone]
            )}
          >
            {stat.count}
          </span>
        </span>
      ))}
    </div>
  );
}
