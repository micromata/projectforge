"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
import { cn } from "@/lib/utils";
import {
  fetchForecastTables,
  type ForecastChartSettings,
  type ForecastInvoiceKind,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { StatisticsTableTabs } from "../statistics/statistics-table-tabs";
import { ForecastInvoiceTable } from "./forecast-invoice-table";
import { ForecastPositionTable } from "./forecast-position-table";
import { ForecastProjectTable } from "./forecast-project-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

/**
 * The rows behind the forecast charts, one sub-tab per sheet of the Excel export. Asked for only once the
 * charts of the same filter and dates are there (`enabled`): the backend has cached that calculation by
 * then, so the tables come without running the forecast a second time.
 */
export function ForecastTables({
  filter,
  filterKey,
  params,
  enabled,
}: {
  filter: MagicFilter;
  /** The stable derivation of `filter` the charts' query key uses. */
  filterKey: string;
  params: ForecastChartSettings;
  enabled: boolean;
}) {
  const t = useStatisticsLabels();
  const query = useQuery({
    queryKey: ["order", "forecastChart", "tables", filterKey, params],
    queryFn: ({ signal }) => fetchForecastTables(filter, params, signal),
    enabled,
    placeholderData: keepPreviousData,
  });
  if (query.isError) {
    return (
      <p className="text-sm text-destructive">
        {query.error instanceof Error
          ? query.error.message
          : String(query.error)}
      </p>
    );
  }
  if (query.isPending) {
    return (
      <div className="flex items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  const tables = query.data;
  const invoices = (kind: ForecastInvoiceKind) =>
    tables.invoices.filter((row) => row.kind === kind);
  const ist = invoices("IST");
  const prevYear = invoices("PREV_YEAR");
  const prevPrevYear = invoices("PREV_PREV_YEAR");
  return (
    <section
      className={cn(
        "space-y-2 transition-opacity",
        query.isPlaceholderData && "opacity-50"
      )}
      aria-busy={query.isPlaceholderData}
    >
      <h3 className="text-sm font-semibold">{t.heading}</h3>
      <StatisticsTableTabs
        tabs={[
          {
            value: "projects",
            label: t.projects,
            count: tables.projects.length,
            content: <ForecastProjectTable tables={tables} />,
          },
          {
            value: "positions",
            label: t.positions,
            count: tables.positions.length,
            content: <ForecastPositionTable tables={tables} />,
          },
          {
            value: "invoices",
            label: t.invoices,
            count: ist.length,
            content: <ForecastInvoiceTable rows={ist} />,
          },
          {
            value: "invoicesPrevYear",
            label: t.invoicesPrevYear,
            count: prevYear.length,
            content: <ForecastInvoiceTable rows={prevYear} />,
          },
          {
            value: "invoicesPrevPrevYear",
            label: t.invoicesPrevPrevYear,
            count: prevPrevYear.length,
            content: <ForecastInvoiceTable rows={prevPrevYear} />,
          },
        ]}
      />
    </section>
  );
}
