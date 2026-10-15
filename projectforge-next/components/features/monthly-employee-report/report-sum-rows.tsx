"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { TableCell, TableRow } from "@/components/ui/table";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { monthlyReportDrillDownHref } from "@/lib/timesheet-links";
import { cn } from "@/lib/utils";
import type { MonthlyReport } from "./types";

/**
 * The footer sum rows of the matrix: the net total (red), the gross total (only when it differs from the
 * net) and the time saved by AI (only when some non-zero saving exists), mirroring the legacy page's three closing rows.
 * Clicking the net total drills down into the time sheet list filtered by the reported user and the month
 * range only (no cost unit / task) — all of the user's sheets for the displayed month; clicking a single
 * week cell of that row narrows the window to just the week.
 */
export function ReportSumRows({ report }: { report: MonthlyReport }) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();

  /** All of the user's sheets over the whole month or — when [weekIndex] is given — just that week. */
  function drillDownAll(weekIndex?: number) {
    const week = weekIndex != null ? report.weeks[weekIndex] : undefined;
    navigate(
      monthlyReportDrillDownHref({
        userId: report.userId ?? 0,
        userName: report.userName,
        startDate: week?.startDate ?? report.startDate,
        endDate: week?.endDate ?? report.endDate,
      })
    );
  }

  return (
    <>
      <SumRow
        report={report}
        title={t("sum")}
        perWeek={report.weeks.map((w) => w.totalDuration)}
        sum={report.totalNetDuration}
        sumClassName="font-bold text-destructive"
        onClick={() => drillDownAll()}
        onWeekClick={drillDownAll}
      />
      {report.showGrossRow && (
        <SumRow
          report={report}
          title={t("fibu.monthlyEmployeeReport.totalSum._")}
          titleTooltip={t("fibu.monthlyEmployeeReport.totalSum.tooltip")}
          perWeek={report.weeks.map((w) => w.grossDuration)}
          sum={report.totalGrossDuration}
          sumClassName="font-bold"
        />
      )}
      {report.hasTimeSavingsByAI && (
        <SumRow
          report={report}
          title={t("timesheet.ai.timeSavedByAI._")}
          perWeek={report.weeks.map((w) => w.timeSavedByAI)}
          sum={report.totalTimeSavedByAI}
          sumClassName="font-bold text-ai-savings"
          trailing={report.timeSavedByAIPercentage}
          accent
        />
      )}
    </>
  );
}

function SumRow({
  report,
  title,
  titleTooltip,
  perWeek,
  sum,
  sumClassName,
  trailing,
  accent,
  onClick,
  onWeekClick,
}: {
  report: MonthlyReport;
  title: string;
  titleTooltip?: string;
  perWeek: string[];
  sum: string;
  sumClassName: string;
  /** The value of the trailing AI column; when omitted it repeats the net total there, as the legacy page. */
  trailing?: string;
  /** Whether the whole row carries the AI (purple) accent. */
  accent?: boolean;
  /** When set, the row is clickable and drills down (net total → all of the user's sheets for the month). */
  onClick?: () => void;
  /** When set, each week cell drills down narrowed to that week (by column index). */
  onWeekClick?: (weekIndex: number) => void;
}) {
  const cellAccent = accent ? "text-ai-savings" : undefined;
  return (
    <TableRow className={cn(onClick && "cursor-pointer")} onClick={onClick}>
      <TableCell
        colSpan={4}
        className={cn("text-right font-bold", accent && "text-ai-savings")}
      >
        {/* A passive label (the row is not actionable), so openOnTap makes the explanation
            reachable on touch too (see HintTooltip). */}
        <HintTooltip text={titleTooltip} plain openOnTap>
          <span>{title}</span>
        </HintTooltip>
      </TableCell>
      {perWeek.map((cell, i) => (
        <TableCell
          key={i}
          className={cn("text-right tabular-nums", cellAccent)}
          onClick={
            onWeekClick
              ? (e) => {
                  // Narrow to this week; don't also fire the row's month drill-down.
                  e.stopPropagation();
                  onWeekClick(i);
                }
              : undefined
          }
        >
          {cell}
        </TableCell>
      ))}
      <TableCell className={cn("text-right tabular-nums", sumClassName)}>
        {sum}
      </TableCell>
      {report.hasTimeSavingsByAI && (
        <TableCell className={cn("text-right tabular-nums", sumClassName)}>
          {trailing ?? report.totalTimeSavedByAI}
        </TableCell>
      )}
    </TableRow>
  );
}
