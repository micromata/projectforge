"use client";

import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";

/**
 * The begin of an absence (an availability entry, a vacation) in a list, as the date cell of the list shows it.
 * Empty for an entry of somebody else shown from today on only (`startHidden`, AbsenceVisibility): its real begin
 * isn't told, and the day sent is just the first one shown.
 */
export function AbsenceStartCell({
  startDate,
  startHidden,
}: {
  startDate?: string | null;
  startHidden?: boolean | null;
}) {
  const format = useFormatContext();
  if (!startDate || startHidden) return null;
  return (
    <span className="text-muted-foreground tabular-nums">
      {formatDate(startDate, format)}
    </span>
  );
}
