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
import { useProjectFocus } from "../statistics/use-project-focus";

type Row = ContributionMarginMonthRow;

/** The sheet Monats-DB: revenue, costs and DB1 per month and project; preliminary months in blue. */
export function ContributionMarginMonthTable({
  rows,
  data,
  focusProjectId,
}: {
  rows: Row[];
  /** The limits of the DB % traffic light. */
  data: ContributionMarginData;
  /** The project whose first month is marked and scrolled to (opened from the project table). */
  focusProjectId?: number | null;
}) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const focusIndex = useProjectFocus(
    rows,
    focusProjectId,
    (row) => row.projectId
  );
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
      moneyColumn<Row>("revenue", t.revenue, (row) => row.revenue, ctx, {
        className: preliminary,
      }),
      moneyColumn<Row>("costs", t.costs, (row) => row.costs, ctx, {
        className: preliminary,
      }),
      moneyColumn<Row>("profit", t.profit, (row) => row.profit, ctx, {
        className: preliminary,
      }),
      {
        id: "percentage",
        accessorFn: (row) => row.percentage ?? undefined,
        header: t.percentage,
        size: 100,
        sortDescFirst: true,
        sortUndefined: "last",
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
  }, [t, ctx, data]);
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(_, index) => String(index)}
      highlightRowId={focusIndex}
    />
  );
}
