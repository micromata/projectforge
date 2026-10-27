"use client";

import { useFormatContext } from "@/hooks/use-format";
import { formatDayHeading } from "@/lib/format";

/** The day the following messages were written on, e.g. "Dienstag, 20. Okt.", centred between them. */
export function DaySeparator({ timestamp }: { timestamp: string }) {
  const formatContext = useFormatContext();
  return (
    <div className="flex justify-center px-3 pt-3 pb-1 text-xs text-muted-foreground">
      {formatDayHeading(timestamp, formatContext)}
    </div>
  );
}
