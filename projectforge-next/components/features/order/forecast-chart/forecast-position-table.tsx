"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateRange, formatPercentageDecimal } from "@/lib/format";
import type { ForecastPositionRow, ForecastTables } from "@/lib/rs/order";
import {
  linkColumn,
  moneyColumn,
  numberColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { formatChartMonth } from "./order-forecast-series";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ForecastPositionRow;

/**
 * The sheet Forecast_Data: one row per order position with its remaining forecast spread over the 12
 * months. The pseudo rows (invoices without order resp. project) carry their invoiced sums only.
 */
export function ForecastPositionTable({ tables }: { tables: ForecastTables }) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const rows = tables.positions;
  const { months } = tables;
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
    const showYear = new Set(months.map((it) => it.slice(0, 4))).size > 1;
    const money = (
      id: string,
      label: string,
      value: (row: Row) => number | null
    ) => moneyColumn<Row>(id, label, value, rows, ctx);
    return [
      linkColumn<Row>(
        "order",
        t.order,
        (row) =>
          row.orderNumber == null
            ? null
            : `${row.orderNumber}${row.positionNumber != null ? `.${row.positionNumber}` : ""}`,
        (row) => (row.orderId != null ? `/order/${row.orderId}` : null),
        80
      ),
      textColumn<Row>(
        "customer",
        t.customer,
        (row) => row.customer,
        160,
        t.total
      ),
      textColumn<Row>("project", t.project, (row) => row.project, 180),
      textColumn<Row>(
        "title",
        t.title,
        (row) => [row.title, row.positionTitle].filter(Boolean).join(" – "),
        240
      ),
      textColumn<Row>("art", t.art, (row) => row.art, 120),
      textColumn<Row>(
        "paymentType",
        t.paymentType,
        (row) => row.paymentType,
        140
      ),
      textColumn<Row>(
        "orderStatus",
        t.orderStatus,
        (row) => row.orderStatus,
        120
      ),
      textColumn<Row>(
        "positionStatus",
        t.positionStatus,
        (row) => row.positionStatus,
        120
      ),
      numberColumn<Row>(
        "personDays",
        t.personDays,
        (row) => row.personDays,
        ctx,
        70
      ),
      money("netSum", t.netSum, (row) => row.netSum),
      {
        id: "probability",
        accessorKey: "probability",
        header: t.probability,
        size: 90,
        sortDescFirst: true,
        meta: { label: t.probability, align: "right" },
        cell: ({ row }) => (
          <span className="tabular-nums">
            {formatPercentageDecimal(row.original.probability, ctx, 0)}
          </span>
        ),
      },
      money("weightedNetSum", t.weightedNetSum, (row) => row.weightedNetSum),
      money("invoicedSum", t.invoicedSum, (row) => row.invoicedSum),
      money("toBeInvoicedSum", t.toBeInvoicedSum, (row) => row.toBeInvoicedSum),
      textColumn<Row>(
        "periodOfPerformance",
        t.periodOfPerformance,
        (row) =>
          formatDateRange(
            row.periodOfPerformanceBegin,
            row.periodOfPerformanceEnd,
            ctx
          ),
        180
      ),
      textColumn<Row>(
        "forecastType",
        t.forecastType,
        (row) => row.forecastType,
        120
      ),
      ...months.map((month, index) =>
        money(
          `month${index}`,
          formatChartMonth(month, ctx, showYear),
          (row) => row.months[index]
        )
      ),
      money("remaining", t.remaining, (row) => row.remaining),
      money("difference", t.difference, (row) => row.difference),
      {
        id: "warning",
        accessorFn: (row) => row.warning ?? "",
        header: t.warning,
        size: 200,
        meta: { label: t.warning },
        cell: ({ row }) => (
          <HintTooltip text={row.original.warning ?? undefined}>
            <span className="truncate text-destructive">
              {row.original.warning}
            </span>
          </HintTooltip>
        ),
      },
    ];
  }, [t, ctx, rows, months]);
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(_, index) => String(index)}
      rowClassName={(row) => (row.pseudo ? "italic" : undefined)}
    />
  );
}
