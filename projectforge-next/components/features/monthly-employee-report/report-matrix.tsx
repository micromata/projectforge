"use client";

import { useTranslations } from "next-intl";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { monthlyReportDrillDownHref } from "@/lib/timesheet-links";
import { ReportSumRows } from "./report-sum-rows";
import type {
  MonthlyReport,
  MonthlyReportRow,
  MonthlyReportWeek,
} from "./types";

/**
 * The report matrix: rows are cost units / tasks, columns the calendar weeks, plus a monthly sum and — when
 * some non-zero saving exists — the time saved by AI. A data row drills down into the time sheet list filtered by the reported
 * user, the exact cost unit (or task) and the month range; clicking a single week cell narrows that window
 * to just the week (see lib/timesheet-links.ts).
 */
export function ReportMatrix({ report }: { report: MonthlyReport }) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();

  /** Drill down for [row], over the whole month or — when [week] is given — just that week's date range. */
  function drillDown(row: MonthlyReportRow, week?: MonthlyReportWeek) {
    navigate(
      monthlyReportDrillDownHref({
        userId: report.userId ?? 0,
        userName: report.userName,
        kost2Id: row.kost2Id ?? undefined,
        kost2Label: row.kost2Id ? row.label : undefined,
        taskId: row.taskId ?? undefined,
        taskName: row.taskId ? row.label : undefined,
        startDate: week?.startDate ?? report.startDate,
        endDate: week?.endDate ?? report.endDate,
      })
    );
  }

  return (
    <div className="overflow-x-auto rounded-md border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            {report.hasKost2Rows ? (
              <>
                <TableHead>{t("fibu.kost2._")}</TableHead>
                <TableHead>{t("fibu.kunde._")}</TableHead>
                <TableHead>{t("fibu.projekt._")}</TableHead>
                <TableHead>{t("fibu.kost2.art")}</TableHead>
              </>
            ) : (
              <TableHead colSpan={4}>{t("task._")}</TableHead>
            )}
            {report.weeks.map((week, i) => (
              <TableHead key={i} className="text-right">
                {`${week.fromDay}.-${week.toDay}.`}
              </TableHead>
            ))}
            <TableHead className="text-right">{t("sum")}</TableHead>
            {report.hasTimeSavingsByAI && (
              <TableHead className="text-right">
                {t("timesheet.ai.timeSavedByAI._")}
              </TableHead>
            )}
          </TableRow>
        </TableHeader>
        <TableBody>
          {report.rows.map((row, i) => (
            <TableRow
              key={i}
              className="cursor-pointer"
              onClick={() => drillDown(row)}
            >
              <LabelCells row={row} />
              {row.perWeek.map((cell, j) => (
                <TableCell
                  key={j}
                  className="text-right tabular-nums"
                  onClick={(e) => {
                    // Narrow the drill-down to this week; don't also trigger the row's month drill-down.
                    e.stopPropagation();
                    drillDown(row, report.weeks[j]);
                  }}
                >
                  {cell}
                </TableCell>
              ))}
              <TableCell className="text-right font-medium tabular-nums">
                {row.sum}
              </TableCell>
              {report.hasTimeSavingsByAI && (
                <TableCell className="text-right tabular-nums text-ai-savings">
                  {row.aiTimeSavings}
                </TableCell>
              )}
            </TableRow>
          ))}
          <ReportSumRows report={report} />
        </TableBody>
      </Table>
    </div>
  );
}

/** The one-to-four label columns of a data row (see the legacy page's addLabelCols). */
function LabelCells({ row }: { row: MonthlyReportRow }) {
  if (row.type !== "kost2") {
    // A task or the pseudo task ("******"): the label takes all four columns.
    return <TableCell colSpan={4}>{row.label}</TableCell>;
  }
  return (
    <>
      <TableCell className="whitespace-nowrap tabular-nums">
        {row.label}
      </TableCell>
      {row.project != null ? (
        <>
          <TableCell>{row.customer}</TableCell>
          <TableCell>{row.project}</TableCell>
        </>
      ) : (
        // No project: the cost unit's description takes the customer+project span.
        <TableCell colSpan={2}>{row.description}</TableCell>
      )}
      <TableCell>{row.kost2Art}</TableCell>
    </>
  );
}
