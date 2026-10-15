"use client";

import type { ForecastWarning } from "@/lib/rs/order";

/**
 * The lost budget warnings of a project as a two-column table: the position, then its warning, so a
 * wrapped warning stays aligned with its first line.
 */
export function ForecastWarningList({
  warnings,
}: {
  warnings: ForecastWarning[];
}) {
  return (
    <div className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
      {warnings.map((warning, index) => (
        <div key={index} className="contents">
          <span className="whitespace-nowrap font-semibold tabular-nums">
            {warning.position}
          </span>
          <span>{warning.text}</span>
        </div>
      ))}
    </div>
  );
}
