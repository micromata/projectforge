"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ContributionMarginInvoiceRow } from "@/lib/rs/order";
import {
  dateColumn,
  linkColumn,
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { PRELIMINARY_CLASS } from "./contribution-margin-preliminary";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

type Row = ContributionMarginInvoiceRow;

/**
 * The sheet Rechnungen: the invoice positions of the projects in the period. Those not booked yet count as
 * preliminary revenue and are shown in blue; the booked ones carry the date of their accounting record.
 */
export function ContributionMarginInvoiceTable({ rows }: { rows: Row[] }) {
  const t = useStatisticsLabels();
  const ctx = useFormatContext();
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      linkColumn<Row>(
        "invoice",
        t.invoice,
        (row) =>
          row.number == null
            ? null
            : `${row.number}${row.positionNumber != null ? `#${row.positionNumber}` : ""}`,
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
      textColumn<Row>("kost", t.kost, (row) => row.kost, 90),
      textColumn<Row>("subject", t.subject, (row) => row.subject, 240),
      textColumn<Row>("status", t.status, (row) => row.status, 110),
      linkColumn<Row>(
        "order",
        t.order,
        (row) => row.order,
        (row) => (row.orderId != null ? `/order/${row.orderId}` : null),
        90
      ),
      dateColumn<Row>(
        "bookedDate",
        t.bookedDate,
        (row) => row.bookedDate,
        ctx,
        110
      ),
      moneyColumn<Row>("netSum", t.net, (row) => row.netSum, ctx, {
        className: (row) => (row.preliminary ? PRELIMINARY_CLASS : undefined),
      }),
    ],
    [t, ctx]
  );
  return (
    <StatisticsTable<Row>
      columns={columns}
      data={rows}
      getRowId={(_, index) => String(index)}
    />
  );
}
