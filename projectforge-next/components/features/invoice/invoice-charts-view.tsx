"use client";

import { useMemo } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRememberedFilter } from "@/components/data-table/use-remembered-filter";
import { AppliedFilterSummary } from "@/components/shared/chart/applied-filter-summary";
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
 * (`OutgoingInvoiceEntityRest.netSumChart`). Criteria of an invoice's current state (paid, status, ...)
 * are left out of every year, and flagged as such in the summary.
 *
 * The filter is the list's live one from the local cache rather than a fresh read of the backend's copy,
 * which can lag behind what the list shows (see useRememberedFilter's `fresh`), and it is spelled out above
 * the charts ({@link AppliedFilterSummary}), so a figure that disagrees with the list can be traced.
 */
export function InvoiceChartsView() {
  const remembered = useRememberedFilter(INVOICE_ENTITY, { fresh: false });
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

  // The invoice date is the reference period; the older years are that period shifted back.
  const compared = query.data?.series
    .filter((series) => series.offset > 0)
    .map((series) => series.label)
    .join(", ");
  const notes = useMemo(
    () =>
      compared
        ? { [INVOICE_DATE_FIELD]: t("comparedPeriods", { arg0: compared }) }
        : undefined,
    [compared, t]
  );
  // The criteria of the invoice's current state (paid, status, ...) are left out of every year, as they
  // make no sense across years; the summary strikes them through.
  const ignored = query.data?.ignoredFilterFields;
  const usage = useMemo(() => ignored && { ignored }, [ignored]);

  return (
    <div className="space-y-8 p-4">
      {/* Above everything, the hint for a missing range included: which of the list's criteria the
          figures rest on is the first thing to check when they disagree with the list. */}
      <AppliedFilterSummary
        entity={INVOICE_ENTITY}
        filter={filter}
        notes={notes}
        usage={usage}
        ignoredTooltip={t("ignoredTooltip")}
      />
      {query.isError ? (
        <p className="text-sm text-destructive">
          {query.error instanceof Error
            ? query.error.message
            : String(query.error)}
        </p>
      ) : query.isPending ? (
        <div className="flex flex-1 items-center justify-center p-8">
          <Spinner />
        </div>
      ) : query.data.months.length === 0 ? (
        // Empty when the invoice-date filter is not a bounded range: "the same period n years earlier"
        // needs a start and an end, so the tab asks for one instead of drawing an empty axis (see
        // netSumChart).
        <p className="text-sm text-muted-foreground">{t("boundedRangeHint")}</p>
      ) : (
        <>
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{t("cumulative")}</h3>
            <InvoiceCumulativeNetSumChart data={query.data} />
          </section>
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{t("monthly")}</h3>
            <InvoiceMonthlyNetSumChart data={query.data} />
          </section>
        </>
      )}
    </div>
  );
}

/** The invoice-date filter the four years are shifted from (`OutgoingInvoiceEntityRest.DATE_FIELD`). */
const INVOICE_DATE_FIELD = "datum";
