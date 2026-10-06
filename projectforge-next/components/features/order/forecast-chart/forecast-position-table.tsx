"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateRange, formatPercentageDecimal } from "@/lib/format";
import type { ForecastPositionRow, ForecastTables } from "@/lib/rs/order";
import {
  linkColumn,
  moneyColumn,
  numberColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import {
  OrderForecastButton,
  type ForecastOrderRef,
} from "../forecast/order-forecast-dialog";
import { StatisticsHintCell } from "../statistics/statistics-hint-cell";
import { StatisticsTable } from "../statistics/statistics-table";
import { formatChartMonth } from "./order-forecast-series";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";
import { useProjectFocus } from "../statistics/use-project-focus";

type Row = ForecastPositionRow;

/**
 * The sheet Forecast_Data: one row per order position with its remaining forecast spread over the 12
 * months. The pseudo rows (invoices without order resp. project) carry their invoiced sums only.
 * The icon behind an order number opens the forecast analysis of the order (`onOpenForecast`).
 */
export function ForecastPositionTable({
  tables,
  focusProjectId,
  onOpenForecast,
}: {
  tables: ForecastTables;
  onOpenForecast?: (order: ForecastOrderRef) => void;
  /**
   * The project whose first position with a warning (else with a difference, else its first one) is
   * marked and scrolled to (opened from the project overview).
   */
  focusProjectId?: number | null;
}) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const rows = tables.positions;
  const { months } = tables;
  const focusIndex = useProjectFocus(
    rows,
    focusProjectId,
    // Positions without project belong to PROJECT_ID_NONE (-1).
    (row) => row.projectId ?? -1,
    // What made the project stand out in the overview: its warnings, then its differences.
    [(row) => row.warning != null, (row) => row.difference !== 0]
  );
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
    const showYear = new Set(months.map((it) => it.slice(0, 4))).size > 1;
    const money = (
      id: string,
      label: string,
      value: (row: Row) => number | null
    ) => moneyColumn<Row>(id, label, value, ctx);
    return [
      linkColumn<Row>(
        "order",
        t.order,
        (row) =>
          row.orderNumber == null
            ? null
            : `${row.orderNumber}${row.positionNumber != null ? `.${row.positionNumber}` : ""}`,
        (row) => (row.orderId != null ? `/order/${row.orderId}` : null),
        100,
        (row) =>
          onOpenForecast && row.orderId != null && row.orderNumber != null ? (
            <OrderForecastButton
              order={{ id: row.orderId, label: String(row.orderNumber) }}
              label={t.forecastDetails}
              onOpen={onOpenForecast}
            />
          ) : null
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
        // In percent, as shown, so the number filter compares what the user reads.
        accessorFn: (row) => row.probability * 100,
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
      // A month with a lost budget warning is marked red, as its cell in the Excel.
      ...months.map((month, index) =>
        moneyColumn<Row>(
          `month${index}`,
          formatChartMonth(month, ctx, showYear),
          (row) => row.months[index],
          ctx,
          {
            className: (row) =>
              row.warningMonths.includes(index)
                ? "rounded-sm bg-destructive/15 px-1 font-semibold text-destructive"
                : undefined,
          }
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
        cell: ({ row }) =>
          row.original.warning ? (
            <StatisticsHintCell
              hint={row.original.warning}
              className="text-destructive"
            >
              {row.original.warning}
            </StatisticsHintCell>
          ) : null,
      },
    ];
  }, [t, ctx, months, onOpenForecast]);
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(_, index) => String(index)}
      rowClassName={(row) => (row.pseudo ? "italic" : undefined)}
      highlightRowId={focusIndex}
    />
  );
}
