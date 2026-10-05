"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatYearMonth } from "@/lib/format";
import type { ContributionMarginTimesheetRow } from "@/lib/rs/order";
import {
  moneyColumn,
  numberColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { PRELIMINARY_CLASS } from "./contribution-margin-preliminary";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ContributionMarginTimesheetRow;

/**
 * The sheet DB-Zeitberichte: the hours booked per month and kost2 in the months without accounting
 * records, valued with the hourly rate as preliminary costs.
 */
export function ContributionMarginTimesheetTable({ rows }: { rows: Row[] }) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      {
        id: "month",
        accessorKey: "month",
        header: t.month,
        size: 140,
        meta: { label: t.month },
        cell: ({ row }) => formatYearMonth(row.original.month, ctx),
        footer: t.total,
      },
      textColumn<Row>("customer", t.customer, (row) => row.customer, 180),
      textColumn<Row>("project", t.project, (row) => row.project, 220),
      textColumn<Row>("kost2", t.kost2, (row) => row.kost2, 110),
      numberColumn<Row>("hours", t.hours, (row) => row.hours, ctx),
      moneyColumn<Row>("costs", t.costs, (row) => row.costs, rows, ctx, {
        className: () => PRELIMINARY_CLASS,
      }),
    ],
    [t, ctx, rows]
  );
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(row) => `${row.month}-${row.projectId}-${row.kost2}`}
    />
  );
}
