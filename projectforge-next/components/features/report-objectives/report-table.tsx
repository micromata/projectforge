"use client";

import { useState } from "react";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon, Note01Icon } from "@hugeicons/core-free-icons";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";
import { recordsHref, visibleRows } from "./rows";
import type { ReportColumn, ReportData, ReportRow } from "./types";

/**
 * The BWA table of the current report: its own assessment as first column, one column per child report.
 * A child with children of its own is a link into it, the path above leads back up. With the right to see
 * accounting records, the amounts and the icon of a column head open the records behind them.
 */
export function ReportTable({
  report,
  canShowRecords,
  busy,
  onSelect,
}: {
  report: ReportData;
  canShowRecords: boolean;
  busy: boolean;
  onSelect: (reportId: string) => void;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const [showAll, setShowAll] = useState(false);
  const rows = visibleRows(report.rows, showAll);

  // Blank for a null/zero amount, as the BWA of the accounting-record list shows it.
  const amountText = (row: ReportRow, amount: number | null) => {
    if (amount == null || amount === 0) return "";
    const number = formatNumber(amount, format, row.scale);
    return row.unit ? `${number} ${row.unit}` : number;
  };

  const columnHead = (column: ReportColumn, index: number) => {
    const id = column.id ?? "";
    // The first column is the current report itself, so only the children lead somewhere.
    const label =
      index > 0 && column.hasChildren ? (
        <button
          type="button"
          className="font-semibold text-primary underline-offset-2 hover:underline disabled:opacity-50"
          disabled={busy}
          title={column.title ?? undefined}
          onClick={() => onSelect(id)}
        >
          {id}
        </button>
      ) : (
        <span className="font-semibold" title={column.title ?? undefined}>
          {id}
        </span>
      );
    return (
      <span className="inline-flex items-center gap-1">
        {label}
        {canShowRecords && (
          <GuardedLink
            href={recordsHref(id)}
            className="text-muted-foreground hover:text-primary"
            aria-label={`${t("fibu.kost.reporting.showRecords")}: ${id}`}
            title={t("fibu.kost.reporting.showRecords")}
          >
            <HugeiconsIcon icon={Note01Icon} size={13} />
          </GuardedLink>
        )}
      </span>
    );
  };

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <div className="flex flex-col gap-0.5">
          <h2 className="text-base font-semibold">
            {report.id} – {report.title}
            {report.period && (
              <span className="font-normal text-muted-foreground">
                {": "}
                {report.period}
              </span>
            )}
          </h2>
          {report.path.length > 0 && (
            <nav
              aria-label={t("fibu.kost.reporting")}
              className="flex flex-wrap items-center gap-1 text-xs"
            >
              {report.path.map((entry) => (
                <span key={entry.id} className="inline-flex items-center gap-1">
                  <button
                    type="button"
                    className="text-primary underline-offset-2 hover:underline disabled:opacity-50"
                    disabled={busy}
                    title={entry.title ?? undefined}
                    onClick={() => onSelect(entry.id ?? "")}
                  >
                    {entry.id}
                  </button>
                  <HugeiconsIcon
                    icon={ArrowRight01Icon}
                    size={12}
                    className="text-muted-foreground"
                  />
                </span>
              ))}
              <span className="font-medium">{report.id}</span>
            </nav>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Switch
            id="report-objectives-show-all-rows"
            checked={showAll}
            onCheckedChange={setShowAll}
          />
          <Label htmlFor="report-objectives-show-all-rows">
            {t("fibu.kost.reporting.showAllRows")}
          </Label>
        </div>
      </div>
      <div className="overflow-x-auto rounded-md border bg-card">
        <table className="w-auto text-xs leading-tight">
          <thead className="border-b bg-muted/40">
            <tr>
              <th className="px-3 py-1.5" />
              <th className="px-2 py-1.5" />
              {report.columns.map((column, index) => (
                <th
                  key={column.id ?? index}
                  className="px-3 py-1.5 text-right whitespace-nowrap"
                >
                  {columnHead(column, index)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, rowIndex) => {
              const emphasized = row.indent === 0 && !!row.title;
              return (
                <tr
                  key={row.id ?? row.no ?? rowIndex}
                  className="even:bg-muted/20"
                >
                  <td className="w-14 px-3 py-0.5 text-muted-foreground tabular-nums">
                    {row.no}
                  </td>
                  <td
                    className={cn(
                      "min-w-64 px-2 py-0.5",
                      emphasized && "font-semibold"
                    )}
                    style={{ paddingLeft: `${0.5 + row.indent}rem` }}
                  >
                    {row.title}
                  </td>
                  {report.columns.map((column, index) => {
                    const amount = row.amounts[index] ?? null;
                    const text = amountText(row, amount);
                    return (
                      <td
                        key={column.id ?? index}
                        className={cn(
                          "px-3 py-0.5 text-right tabular-nums whitespace-nowrap",
                          index === 0 && "font-semibold",
                          amount != null && amount < 0 && "text-destructive"
                        )}
                      >
                        {text && canShowRecords && column.id ? (
                          <GuardedLink
                            href={recordsHref(column.id, row.no)}
                            className="underline-offset-2 hover:underline"
                            aria-label={`${t("fibu.kost.reporting.showRecords")}: ${column.id}, ${row.title ?? row.no}`}
                          >
                            {text}
                          </GuardedLink>
                        ) : (
                          text
                        )}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
