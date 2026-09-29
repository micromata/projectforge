"use client";

import { useMemo } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRememberedFilter } from "@/components/data-table/use-remembered-filter";
import { Spinner } from "@/components/shared/spinner";
import { fetchInvoiceNetSumChart } from "@/lib/rs/invoice";
import type { MagicFilter } from "@/lib/rs/types";
import { INVOICE_ENTITY } from "./invoice.page";
import { InvoiceCumulativeNetSumChart } from "./invoice-cumulative-net-sum-chart";
import { InvoiceMonthlyNetSumChart } from "./invoice-monthly-net-sum-chart";

/**
 * The "Grafiken" tab of `/invoice` (see `app/(authenticated)/invoice/page.tsx`): the filtered invoices'
 * monthly net sums, this year and the three before it, as grouped bars and as cumulative curves.
 *
 * Reads the very filter the list is showing through {@link useRememberedFilter} — the channel the list
 * itself restores from and the toolbar keeps live — so the charts follow the list without a filter bar of
 * their own. The four years are that filter's invoice-date range shifted zero to three years back, which
 * the backend computes with the same shift as the statistics line's previous-year comparison
 * (`OutgoingInvoiceEntityRest.netSumChart`).
 */
export function InvoiceChartsView() {
  const remembered = useRememberedFilter(INVOICE_ENTITY);
  if (remembered.isPending) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  return <InvoiceCharts filter={remembered.filter} />;
}

function InvoiceCharts({ filter }: { filter: MagicFilter | undefined }) {
  const t = useTranslations("fibu.rechnung.chart");
  // The filter drives the query key, so a changed list filter refetches when the user returns to this tab.
  const key = useMemo(() => JSON.stringify(filter ?? {}), [filter]);
  const query = useQuery({
    queryKey: ["outgoingInvoice", "netSumChart", key],
    queryFn: ({ signal }) =>
      fetchInvoiceNetSumChart(
        filter ?? { entries: [], sortProperties: [] },
        signal
      ),
    placeholderData: keepPreviousData,
  });

  if (query.isError) {
    return (
      <p className="p-4 text-sm text-destructive">
        {query.error instanceof Error
          ? query.error.message
          : String(query.error)}
      </p>
    );
  }
  if (query.isPending) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  // Empty when the invoice-date filter is not a bounded range: "the same period n years earlier" needs a
  // start and an end, so the tab asks for one instead of drawing an empty axis (see netSumChart).
  if (query.data.months.length === 0) {
    return (
      <p className="p-4 text-sm text-muted-foreground">
        {t("boundedRangeHint")}
      </p>
    );
  }
  return (
    <div className="space-y-8 p-4">
      <section className="space-y-2">
        <h3 className="text-sm font-semibold">{t("cumulative")}</h3>
        <InvoiceCumulativeNetSumChart data={query.data} />
      </section>
      <section className="space-y-2">
        <h3 className="text-sm font-semibold">{t("monthly")}</h3>
        <InvoiceMonthlyNetSumChart data={query.data} />
      </section>
    </div>
  );
}
