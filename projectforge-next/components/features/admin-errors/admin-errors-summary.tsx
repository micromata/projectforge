"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type { LogGroupSummary } from "@/lib/rs/admin-errors";
import { cn } from "@/lib/utils";

/** The key figures of all problems, whatever the filter: what happened within the last day, what needs a look. */
export function AdminErrorsSummary({ summary }: { summary: LogGroupSummary }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const tiles: { label: string; value: number; alert?: boolean }[] = [
    {
      label: t("system.admin.adminErrors.kpi.occurrences"),
      value: summary.occurrences24h,
    },
    {
      label: t("system.admin.adminErrors.kpi.newProblems"),
      value: summary.newProblems24h,
      alert: true,
    },
    {
      label: t("system.admin.adminErrors.kpi.regressions"),
      value: summary.regressions,
      alert: true,
    },
    {
      label: t("system.admin.adminErrors.kpi.external"),
      value: summary.externalProblems24h,
      alert: true,
    },
    { label: t("system.admin.adminErrors.kpi.open"), value: summary.open },
  ];
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5">
      {tiles.map((tile) => (
        <div key={tile.label} className="rounded-md border px-3 py-2">
          <div className="text-xs text-muted-foreground">{tile.label}</div>
          <div
            className={cn(
              "text-2xl font-semibold tabular-nums",
              tile.alert && tile.value > 0 && "text-destructive"
            )}
          >
            {formatNumber(tile.value, ctx, 0)}
          </div>
        </div>
      ))}
    </div>
  );
}
