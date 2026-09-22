"use client";

import { useTranslations } from "next-intl";
import { TableCell, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { MonthlyReport } from "./types";

/**
 * The footer sum rows of the matrix: the net total (red), the gross total (only when it differs from the
 * net) and the time saved by AI (only when enabled), mirroring the legacy page's three closing rows.
 */
export function ReportSumRows({ report }: { report: MonthlyReport }) {
  const t = useTranslations();
  return (
    <>
      <SumRow
        report={report}
        title={t("sum")}
        perWeek={report.weeks.map((w) => w.totalDuration)}
        sum={report.totalNetDuration}
        sumClassName="font-bold text-destructive"
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
      {report.timeSavingsByAIEnabled && (
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
}) {
  const cellAccent = accent ? "text-ai-savings" : undefined;
  return (
    <TableRow>
      <TableCell
        colSpan={4}
        className={cn("text-right font-bold", accent && "text-ai-savings")}
        title={titleTooltip}
      >
        {title}
      </TableCell>
      {perWeek.map((cell, i) => (
        <TableCell
          key={i}
          className={cn("text-right tabular-nums", cellAccent)}
        >
          {cell}
        </TableCell>
      ))}
      <TableCell className={cn("text-right tabular-nums", sumClassName)}>
        {sum}
      </TableCell>
      {report.timeSavingsByAIEnabled && (
        <TableCell className={cn("text-right tabular-nums", sumClassName)}>
          {trailing ?? report.totalTimeSavedByAI}
        </TableCell>
      )}
    </TableRow>
  );
}
