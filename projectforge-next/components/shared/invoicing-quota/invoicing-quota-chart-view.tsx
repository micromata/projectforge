"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { fetchInvoicingQuotaHistory } from "@/lib/rs/monthly-employee-report";
import { InvoicingQuotaChart } from "./invoicing-quota-chart";

/**
 * The "Fakturaquote" tab (monthly report and personal statistics): the quota of the 12 months ending with
 * [year]/[month] for [userId]; omitted parts default to the logged-in user / current month. The monthly
 * report passes its resolved user/year/month (not the raw filter, whose parts may still be unset while the
 * backend restores the last selection), so it follows every filter change.
 */
export function InvoicingQuotaChartView({
  userId,
  year,
  month,
}: {
  userId?: number | null;
  year?: number;
  month?: number;
}) {
  const t = useTranslations("fibu.monthlyEmployeeReport.invoicingQuotaChart");
  const query = useQuery({
    queryKey: [
      "monthlyEmployeeReport",
      "invoicingQuotaHistory",
      userId ?? null,
      year ?? null,
      month ?? null,
    ],
    queryFn: ({ signal }) =>
      fetchInvoicingQuotaHistory(
        {
          userId: userId ?? undefined,
          year,
          month,
        },
        signal
      ),
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
  if (query.data.months.every((month) => month.quota == null)) {
    return <p className="text-sm text-muted-foreground">{t("noData")}</p>;
  }
  return (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold">{t("title")}</h3>
      <InvoicingQuotaChart data={query.data} />
    </section>
  );
}
