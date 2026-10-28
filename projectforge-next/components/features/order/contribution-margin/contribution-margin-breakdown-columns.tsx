"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type {
  ContributionMarginBreakdown,
  ContributionMarginBreakdownRow,
} from "@/lib/rs/order";
import { ContributionMarginBreakdownNameCell } from "./contribution-margin-breakdown-name-cell";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";

type Row = ContributionMarginBreakdownRow;

/**
 * The columns of the breakdown tree: the indented name with its expand button, then the amounts of the period
 * and the previous year. Not sortable nor filterable, as that would break the tree.
 */
export function useBreakdownColumns(
  data: ContributionMarginBreakdown,
  expanded: Set<string>,
  onToggle: (id: string) => void
): ColumnDef<Row, unknown>[] {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  return useMemo<ColumnDef<Row, unknown>[]>(() => {
    const money = (value: number) => (
      <span className={cn("tabular-nums", value < 0 && "text-destructive")}>
        {formatCurrency(value, ctx)}
      </span>
    );
    const column = (
      id: string,
      label: string,
      cell: (row: Row) => React.ReactNode,
      size = 120
    ): ColumnDef<Row, unknown> => ({
      id,
      header: label,
      size,
      enableSorting: false,
      enableColumnFilter: false,
      meta: { label, align: "right" },
      cell: ({ row }) => cell(row.original),
    });
    const name =
      data.level === "ORDER"
        ? t("breakdown.column.order")
        : t("breakdown.column.task");
    return [
      {
        id: "name",
        header: name,
        size: 420,
        enableSorting: false,
        enableColumnFilter: false,
        meta: { label: name },
        cell: ({ row }) => (
          <ContributionMarginBreakdownNameCell
            row={row.original}
            level={data.level}
            expanded={expanded.has(row.original.id)}
            onToggle={onToggle}
          />
        ),
      },
      column("revenue", t("revenue"), (row) => money(row.series.sums.revenue)),
      column("costs", t("costs"), (row) => money(row.series.sums.costs)),
      column("profit", t("profit"), (row) => money(row.series.sums.profit)),
      column(
        "percentage",
        t("percentage"),
        (row) => (
          <ContributionMarginPercentage
            percentage={row.series.sums.percentage}
            costs={row.series.sums.costs}
            limits={data}
          />
        ),
        100
      ),
      column("prevYear", t("prevYear"), (row) =>
        money(row.series.sums.prevYearProfit)
      ),
      column(
        "prevYearPercentage",
        t("prevYearPercentage"),
        (row) => (
          <ContributionMarginPercentage
            percentage={row.series.sums.prevYearPercentage}
            costs={Math.max(0, -row.series.sums.prevYearProfit)}
            limits={data}
          />
        ),
        100
      ),
    ];
  }, [t, ctx, data, expanded, onToggle]);
}
