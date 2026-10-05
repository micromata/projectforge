"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatYearMonth } from "@/lib/format";
import type {
  ContributionMarginData,
  ContributionMarginMonthRow,
} from "@/lib/rs/order";
import {
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";
import { PRELIMINARY_CLASS } from "./contribution-margin-preliminary";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ContributionMarginMonthRow;

/** The sheet Monats-DB: revenue, costs and DB1 per month and project; preliminary months in blue. */
export function ContributionMarginMonthTable({
  rows,
  data,
}: {
  rows: Row[];
  /** The limits of the DB % traffic light. */
  data: ContributionMarginData;
}) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
    const preliminary = (row: Row) =>
      row.preliminary ? PRELIMINARY_CLASS : undefined;
    return [
      {
        id: "month",
        accessorKey: "month",
        header: t.month,
        size: 140,
        meta: { label: t.month },
        cell: ({ row }) => (
          <span className={preliminary(row.original)}>
            {formatYearMonth(row.original.month, ctx)}
          </span>
        ),
        footer: t.total,
      },
      textColumn<Row>("customer", t.customer, (row) => row.customer, 180),
      textColumn<Row>("project", t.project, (row) => row.project, 220),
      textColumn<Row>("kost", t.kost, (row) => row.kost, 90),
      moneyColumn<Row>("revenue", t.revenue, (row) => row.revenue, rows, ctx, {
        className: preliminary,
      }),
      moneyColumn<Row>("costs", t.costs, (row) => row.costs, rows, ctx, {
        className: preliminary,
      }),
      moneyColumn<Row>("profit", t.profit, (row) => row.profit, rows, ctx, {
        className: preliminary,
      }),
      {
        id: "percentage",
        accessorFn: (row) => row.percentage ?? Number.NEGATIVE_INFINITY,
        header: t.percentage,
        size: 100,
        sortDescFirst: true,
        meta: { label: t.percentage, align: "right" },
        cell: ({ row }) => (
          <ContributionMarginPercentage
            percentage={row.original.percentage}
            costs={row.original.costs}
            limits={data}
          />
        ),
      },
    ];
  }, [t, ctx, rows, data]);
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(row) => `${row.month}-${row.projectId}`}
    />
  );
}
