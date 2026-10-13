"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber, formatPercentageDecimal } from "@/lib/format";
import type {
  LogGroupCounts,
  LogGroupFilter,
  LogGroupSummary,
} from "@/lib/rs/admin-errors";
import { cn } from "@/lib/utils";
import { AdminHandledCounts } from "./admin-handled-counts";

/**
 * The key figures of all problems, whatever the filter: what happened within the last day, what needs a look. Each
 * figure counts the active problems; the resolved, ignored and muted ones are listed below it. A click shows the
 * problems a figure counts ([onOpen] with the list's filter of the figure).
 */
export function AdminErrorsSummary({
  summary,
  onOpen,
}: {
  summary: LogGroupSummary;
  onOpen: (filter: LogGroupFilter) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const activeOccurrences = summary.occurrences24h.active;
  const occurrences = formatNumber(activeOccurrences, ctx, 0);
  // The problems lead: a single problem may occur 100,000 times and hide the others in the sum.
  const problemsDetail =
    summary.problems24h.active > 1 && activeOccurrences > 0
      ? t("system.admin.adminErrors.kpi.problemsTopShare", {
          arg0: occurrences,
          arg1: formatPercentageDecimal(
            summary.topOccurrences24h / activeOccurrences,
            ctx,
            0
          ),
        })
      : t("system.admin.adminErrors.kpi.problemsOccurrences", {
          arg0: occurrences,
        });
  const tiles: {
    label: string;
    value: LogGroupCounts;
    alert?: boolean;
    detail?: string;
    filter: LogGroupFilter;
  }[] = [
    {
      label: t("system.admin.adminErrors.kpi.problems"),
      value: summary.problems24h,
      detail: problemsDetail,
      filter: { status: "ALL", days: 1 },
    },
    {
      label: t("system.admin.adminErrors.kpi.newProblems"),
      value: summary.newProblems24h,
      alert: true,
      filter: { status: "ALL", days: 0, scope: "NEW_24H" },
    },
    {
      label: t("system.admin.adminErrors.kpi.regressions"),
      value: summary.regressions,
      alert: true,
      filter: { status: "ALL", days: 0, scope: "REGRESSION" },
    },
    {
      label: t("system.admin.adminErrors.kpi.external"),
      value: summary.externalProblems24h,
      alert: true,
      filter: { status: "ALL", category: "EXTERNAL", days: 1 },
    },
    {
      label: t("system.admin.adminErrors.kpi.open"),
      value: summary.open,
      filter: { status: "NEW", days: 0 },
    },
  ];
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5">
      {tiles.map((tile) => (
        <button
          key={tile.label}
          type="button"
          onClick={() => onOpen(tile.filter)}
          className="rounded-md border px-3 py-2 text-left transition-colors hover:bg-muted/50"
        >
          <div className="text-xs text-muted-foreground">{tile.label}</div>
          <div
            className={cn(
              "text-2xl font-semibold tabular-nums",
              tile.alert && tile.value.active > 0 && "text-destructive"
            )}
          >
            {formatNumber(tile.value.active, ctx, 0)}
          </div>
          {tile.detail && (
            <div className="text-xs text-muted-foreground tabular-nums">
              {tile.detail}
            </div>
          )}
          <AdminHandledCounts counts={tile.value} />
        </button>
      ))}
    </div>
  );
}
