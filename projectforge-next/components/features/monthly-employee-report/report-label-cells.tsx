"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Alert02Icon } from "@hugeicons/core-free-icons";
import { TableCell } from "@/components/ui/table";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { MonthlyReportRow } from "./types";

/** The one-to-four label columns of a data row (see the legacy page's addLabelCols). */
export function ReportLabelCells({ row }: { row: MonthlyReportRow }) {
  const t = useTranslations();
  if (row.type !== "kost2") {
    // A task or the pseudo task ("******"): the label takes all four columns.
    if (!row.missingKost2) {
      return <TableCell colSpan={4}>{row.label}</TableCell>;
    }
    // Booked without Kost2 while the db-param asks to report such sheets. The row is itself actionable
    // (drill-down), so openOnTap is left off (see HintTooltip).
    return (
      <TableCell colSpan={4}>
        <HintTooltip
          text={t("fibu.monthlyEmployeeReport.missingKost2.tooltip")}
          plain
        >
          <span className="flex items-center gap-1.5">
            <HugeiconsIcon
              icon={Alert02Icon}
              size={16}
              className="shrink-0 text-warning"
              aria-label={t("fibu.monthlyEmployeeReport.missingKost2._")}
            />
            <span>{row.label}</span>
          </span>
        </HintTooltip>
      </TableCell>
    );
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
