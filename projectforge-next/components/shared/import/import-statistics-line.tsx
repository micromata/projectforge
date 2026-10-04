"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { statisticsEntries } from "./import-model";
import type { ImportStorageInfo } from "./import-types";

interface Props {
  info?: ImportStorageInfo;
  /**
   * The status-group keys currently hidden (see STATUS_GROUP_OF). When passed together with
   * [onToggleKey], each per-status count gets a checkbox filtering its rows; without them the line stays
   * plain text.
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
 *
 * As a filter, each count leads with a checkbox (checked = its rows are shown): a plain toggle button did not
 * read as clickable. The label, count included, is the checkbox's label and toggles it as well.
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
        const id = `import-status-filter-${stat.key}`;
        return (
          <span key={stat.key} className="flex items-center gap-1.5">
            <Checkbox
              id={id}
              checked={!hidden}
              onCheckedChange={() => onToggleKey?.(stat.key)}
            />
            <Label
              htmlFor={id}
              className="cursor-pointer gap-1 text-sm font-normal"
            >
              {count}
            </Label>
          </span>
        );
      })}
    </div>
  );
}
