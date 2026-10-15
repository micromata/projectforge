"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef, Table } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type {
  ContributionMarginData,
  ContributionMarginProject,
  ContributionMarginSums,
} from "@/lib/rs/order";
import { StatisticsTable } from "../statistics/statistics-table";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";

type Row = ContributionMarginProject;

/** DB % as the backend computes it (ContributionMarginCalculator.percentage): one decimal, 0 for a loss. */
function percentageOf(revenue: number, profit: number): number | null {
  if (revenue <= 0) return null;
  return Math.max(0, Math.round((profit * 1000) / revenue) / 10);
}

/**
 * The sums of the projects the column filters leave: the backend's own total while nothing is filtered (it
 * is exact), otherwise summed up here with the percentages recomputed from the summed amounts.
 */
function totalOf(
  table: Table<Row>,
  total: ContributionMarginSums
): ContributionMarginSums {
  if (table.getState().columnFilters.length === 0) return total;
  const sum = (key: keyof ContributionMarginSums) =>
    table
      .getFilteredRowModel()
      .rows.reduce((acc, row) => acc + (row.original[key] ?? 0), 0);
  const revenue = sum("revenue");
  const profit = sum("profit");
  const prevYearRevenue = sum("prevYearRevenue");
  const prevYearProfit = sum("prevYearProfit");
  const prevPrevYearRevenue = sum("prevPrevYearRevenue");
  const prevPrevYearProfit = sum("prevPrevYearProfit");
  return {
    revenue,
    costs: sum("costs"),
    profit,
    percentage: percentageOf(revenue, profit),
    prevYearRevenue,
    prevYearProfit,
    prevYearPercentage: percentageOf(prevYearRevenue, prevYearProfit),
    prevPrevYearRevenue,
    prevPrevYearProfit,
    prevPrevYearPercentage: percentageOf(
      prevPrevYearRevenue,
      prevPrevYearProfit
    ),
  };
}

/**
 * The contribution margin per project of the period, with the DB1 and DB % of the two previous years; the sums
 * of the projects left by the column filters in the last row, under their columns. The DB % carries a traffic light by the configured target and red threshold. Sorted by
 * customer and project as the backend sends it; any column sorts on click. A click on a row opens the project's
 * months (Monats-DB).
 */
export function ContributionMarginProjectTable({
  data,
  onProjectClick,
}: {
  data: ContributionMarginData;
  onProjectClick: (projectId: number) => void;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const { total } = data;
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
    const money = (value: number) => (
      <span className={cn("tabular-nums", value < 0 && "text-destructive")}>
        {formatCurrency(value, ctx)}
      </span>
    );
    const text = (
      key: "customer" | "kost",
      label: string,
      size: number
    ): ColumnDef<Row, unknown> => ({
      id: key,
      accessorFn: (row) => row[key] ?? "",
      header: label,
      size,
      meta: { label },
      cell: ({ row }) => row.original[key] ?? "",
      footer: key === "customer" ? t("total") : undefined,
    });
    const amount = (
      key:
        | "revenue"
        | "costs"
        | "profit"
        | "prevYearProfit"
        | "prevPrevYearProfit",
      label: string
    ): ColumnDef<Row, unknown> => ({
      id: key,
      accessorKey: key,
      header: label,
      size: 120,
      sortDescFirst: true,
      meta: { label, align: "right" },
      cell: ({ row }) => money(row.original[key]),
      footer: ({ table }) => money(totalOf(table, total)[key]),
    });
    const percentage = (
      key: "percentage" | "prevYearPercentage" | "prevPrevYearPercentage",
      label: string,
      // The costs, telling a loss without revenue (red "–") from an empty row.
      costs: (row: Row | typeof total) => number
    ): ColumnDef<Row, unknown> => ({
      id: key,
      // Projects without revenue (no percentage) sort below all others.
      accessorFn: (row) => row[key] ?? undefined,
      header: label,
      size: 100,
      sortDescFirst: true,
      sortUndefined: "last",
      meta: { label, align: "right" },
      cell: ({ row }) => (
        <ContributionMarginPercentage
          percentage={row.original[key]}
          costs={costs(row.original)}
          limits={data}
        />
      ),
      footer: ({ table }) => {
        const filtered = totalOf(table, total);
        return (
          <ContributionMarginPercentage
            percentage={filtered[key]}
            costs={costs(filtered)}
            limits={data}
          />
        );
      },
    });
    return [
      text("customer", t("customer"), 180),
      {
        id: "project",
        accessorFn: (row) => row.project ?? "",
        header: t("project"),
        size: 220,
        meta: { label: t("project") },
        cell: ({ row }) => row.original.project ?? row.original.projectId,
      },
      text("kost", t("kost"), 80),
      amount("revenue", t("revenue")),
      amount("costs", t("costs")),
      amount("profit", t("profit")),
      percentage("percentage", t("percentage"), (row) => row.costs),
      amount("prevYearProfit", t("prevYear")),
      percentage("prevYearPercentage", t("prevYearPercentage"), (row) =>
        Math.max(0, -row.prevYearProfit)
      ),
      amount("prevPrevYearProfit", t("prevPrevYear")),
      percentage("prevPrevYearPercentage", t("prevPrevYearPercentage"), (row) =>
        Math.max(0, -row.prevPrevYearProfit)
      ),
    ];
  }, [t, ctx, data, total]);

  return (
    <StatisticsTable<Row>
      columns={columns}
      data={data.projects}
      getRowId={(row) => String(row.projectId)}
      onRowClick={(row) => onProjectClick(row.projectId)}
    />
  );
}
