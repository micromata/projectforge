"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ForecastProjectRow, ForecastTables } from "@/lib/rs/order";
import {
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { ForecastWarningList } from "./forecast-warning-list";
import { StatisticsHintCell } from "../statistics/statistics-hint-cell";
import { StatisticsTable } from "../statistics/statistics-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ForecastProjectRow;

/**
 * The sheet Projektübersicht: forecast (remaining + invoiced), plan and previous years per project, plus
 * the differences and lost budget warnings of its positions, so a project needing a closer look at its
 * positions stands out here already.
 */
export function ForecastProjectTable({
  tables,
  onProjectClick,
}: {
  tables: ForecastTables;
  /** Opens the positions of the project of the clicked row. */
  onProjectClick: (projectId: number) => void;
}) {
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
        cell: ({ row }) => row.original.project ?? row.original.projectId,
      },
      moneyColumn<Row>("forecast", t.forecast, (row) => row.forecast, ctx),
      ...(withPlan
        ? [moneyColumn<Row>("plan", t.plan, (row) => row.plan, ctx)]
        : []),
      moneyColumn<Row>("prevYear", t.prevYear, (row) => row.prevYear, ctx),
      moneyColumn<Row>(
        "prevPrevYear",
        t.prevPrevYear,
        (row) => row.prevPrevYear,
        ctx
      ),
      moneyColumn<Row>(
        "difference",
        t.difference,
        (row) => row.difference,
        ctx
      ),
      {
        id: "warnings",
        // The number of warnings, so the column sorts and filters by it.
        accessorFn: (row) => row.warnings.length || undefined,
        header: t.warning,
        size: 90,
        sortDescFirst: true,
        sortUndefined: "last",
        meta: { label: t.warning, align: "right", filterKind: "number" },
        cell: ({ row }) =>
          row.original.warnings.length > 0 ? (
            <StatisticsHintCell
              hint={<ForecastWarningList warnings={row.original.warnings} />}
              className="justify-end font-semibold tabular-nums text-destructive"
            >
              {row.original.warnings.length}
            </StatisticsHintCell>
          ) : null,
        footer: ({ table }) => {
          const count = table
            .getFilteredRowModel()
            .rows.reduce((acc, it) => acc + it.original.warnings.length, 0);
          return count > 0 ? (
            <span className="tabular-nums text-destructive">{count}</span>
          ) : null;
        },
      },
    ],
    [t, ctx, withPlan]
  );
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(row) => String(row.projectId)}
      onRowClick={(row) => onProjectClick(row.projectId)}
    />
  );
}
