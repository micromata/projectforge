"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ForecastInvoiceRow } from "@/lib/rs/order";
import {
  dateColumn,
  linkColumn,
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import {
  OrderForecastButton,
  type ForecastOrderRef,
} from "../forecast/order-forecast-dialog";
import { StatisticsTable } from "../statistics/statistics-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ForecastInvoiceRow;

/**
 * The invoice positions of one of the invoice sheets (Rechnungen, Rechnungen Vorjahr, Rechnungen
 * Vorvorjahr), already reduced to the projects of the forecast. The icon behind an order opens its forecast
 * analysis (`onOpenForecast`).
 */
export function ForecastInvoiceTable({
  rows,
  onOpenForecast,
}: {
  rows: Row[];
  onOpenForecast?: (order: ForecastOrderRef) => void;
}) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      linkColumn<Row>(
        "invoice",
        t.invoice,
        (row) =>
          row.invoiceNumber == null
            ? null
            : `${row.invoiceNumber}${row.positionNumber != null ? `#${row.positionNumber}` : ""}`,
        (row) => (row.invoiceId != null ? `/invoice/${row.invoiceId}` : null),
        90
      ),
      dateColumn<Row>("date", t.date, (row) => row.date, ctx),
      textColumn<Row>(
        "customer",
        t.customer,
        (row) => row.customer,
        180,
        t.total
      ),
      textColumn<Row>("project", t.project, (row) => row.project, 200),
      textColumn<Row>("subject", t.subject, (row) => row.subject, 240),
      textColumn<Row>(
        "positionText",
        t.positionText,
        (row) => row.positionText,
        240
      ),
      linkColumn<Row>(
        "order",
        t.order,
        (row) => row.order,
        (row) => (row.orderId != null ? `/order/${row.orderId}` : null),
        110,
        (row) =>
          onOpenForecast && row.orderId != null && row.order ? (
            <OrderForecastButton
              order={{ id: row.orderId, label: row.order.split(".")[0] }}
              label={t.forecastDetails}
              onOpen={onOpenForecast}
            />
          ) : null
      ),
      moneyColumn<Row>("netSum", t.net, (row) => row.netSum, ctx),
    ],
    [t, ctx, onOpenForecast]
  );
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(_, index) => String(index)}
    />
  );
}
