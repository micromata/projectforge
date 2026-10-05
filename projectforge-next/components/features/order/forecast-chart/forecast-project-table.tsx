"use client";

import { useMemo } from "react";
import Link from "next/link";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ForecastProjectRow, ForecastTables } from "@/lib/rs/order";
import {
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ForecastProjectRow;

/** The sheet Projektübersicht: forecast (remaining + invoiced), plan and previous years per project. */
export function ForecastProjectTable({ tables }: { tables: ForecastTables }) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const rows = tables.projects;
  const withPlan = rows.some((row) => row.plan != null);
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      textColumn<Row>(
        "customer",
        t.customer,
        (row) => row.customer,
        200,
        t.total
      ),
      {
        id: "project",
        accessorFn: (row) => row.project ?? "",
        header: t.project,
        size: 240,
        meta: { label: t.project },
        cell: ({ row }) =>
          row.original.projectId > 0 ? (
            <Link
              href={`/project/${row.original.projectId}`}
              className="hover:underline"
            >
              {row.original.project ?? row.original.projectId}
            </Link>
          ) : (
            row.original.project
          ),
      },
      moneyColumn<Row>(
        "forecast",
        t.forecast,
        (row) => row.forecast,
        rows,
        ctx
      ),
      ...(withPlan
        ? [moneyColumn<Row>("plan", t.plan, (row) => row.plan, rows, ctx)]
        : []),
      moneyColumn<Row>(
        "prevYear",
        t.prevYear,
        (row) => row.prevYear,
        rows,
        ctx
      ),
      moneyColumn<Row>(
        "prevPrevYear",
        t.prevPrevYear,
        (row) => row.prevPrevYear,
        rows,
        ctx
      ),
    ],
    [t, ctx, rows, withPlan]
  );
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(row) => String(row.projectId)}
    />
  );
}
