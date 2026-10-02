"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type {
  ContributionMarginData,
  ContributionMarginProject,
} from "@/lib/rs/order";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";
import { ContributionMarginTotal } from "./contribution-margin-total";

type Row = ContributionMarginProject;

/**
 * The contribution margin per project of the period, with the DB1 of the two previous years; the sums
 * of all projects below. The DB % carries a traffic light by the configured target and red threshold. Sorted by
 * customer and project as the backend sends it; any column sorts on click.
 */
export function ContributionMarginProjectTable({
  data,
}: {
  data: ContributionMarginData;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
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
      cell: ({ row }) => (
        <span
          className={cn(
            "tabular-nums",
            row.original[key] < 0 && "text-destructive"
          )}
        >
          {formatCurrency(row.original[key], ctx)}
        </span>
      ),
    });
    const percentage = (
      key: "percentage" | "prevYearPercentage",
      label: string,
      // The costs, telling a loss without revenue (red "–") from an empty row.
      costs: (row: Row) => number
    ): ColumnDef<Row, unknown> => ({
      id: key,
      // Projects without revenue (no percentage) sort below all others.
      accessorFn: (row) => row[key] ?? Number.NEGATIVE_INFINITY,
      header: label,
      size: 100,
      sortDescFirst: true,
      meta: { label, align: "right" },
      cell: ({ row }) => (
        <ContributionMarginPercentage
          percentage={row.original[key]}
          costs={costs(row.original)}
          limits={data}
        />
      ),
    });
    return [
      text("customer", t("customer"), 180),
      {
        id: "project",
        accessorFn: (row) => row.project ?? "",
        header: t("project"),
        size: 220,
        meta: { label: t("project") },
        cell: ({ row }) => (
          <Link
            href={`/project/${row.original.projectId}`}
            className="hover:underline"
          >
            {row.original.project ?? row.original.projectId}
          </Link>
        ),
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
    ];
  }, [t, ctx, data]);

  return (
    <DataTable<Row>
      columns={columns}
      data={data.projects}
      enableColumnFilters={false}
      manualSorting={false}
      manualPagination
      showPagination={false}
      autoHeight
      columnLines
      getRowId={(row) => String(row.projectId)}
      footer={<ContributionMarginTotal data={data} />}
    />
  );
}
