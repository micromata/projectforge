"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useTabParam } from "@/hooks/use-tab-param";
import { updateSearchParams } from "@/lib/search-params";
import {
  fetchOrderStatisticsMeta,
  type OrderStatisticsMeta,
} from "@/lib/rs/order-statistics";
import { OrderContributionMarginView } from "../contribution-margin/order-contribution-margin-view";
import { OrderForecastChartsView } from "../forecast-chart/order-forecast-charts-view";
import { OrderStatisticsFilterBar } from "./order-statistics-filter-bar";
import { FROM_ORDER_BOOK_PARAM } from "./order-statistics-url";
import { useOrderStatisticsFilter } from "./use-order-statistics-filter";

/**
 * The order statistics ("Auftragsstatistik", `/finance/statistics`): the forecast charts and, for finance,
 * controlling and project managers (`meta.contributionMargin`), the contribution margin, over a filter
 * of their own (business units, customers, projects) with its own favorites.
 *
 * Opened from the order book (`?fromOrderBook=1`), the backend replaces that filter by the order book's
 * criteria once; the parameter is dropped from the url right away, so a reload keeps what was changed
 * here. Nothing changed here reaches the order book.
 */
export function OrderStatisticsPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [fromOrderBook] = useState(
    () => params.get(FROM_ORDER_BOOK_PARAM) != null
  );
  useEffect(() => {
    if (!fromOrderBook) return;
    updateSearchParams({ [FROM_ORDER_BOOK_PARAM]: null }, "replace");
  }, [fromOrderBook]);

  const meta = useQuery({
    queryKey: ["orderStatistics", "meta", fromOrderBook],
    queryFn: ({ signal }) => fetchOrderStatisticsMeta(fromOrderBook, signal),
    // Seeds the filter of this visit only; a later visit must read the filter stored since.
    gcTime: 0,
    staleTime: Infinity,
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.projectmanagement")}
        title={t("menu.fibu.orderStatistics")}
      />
      {meta.isError ? (
        <p className="p-4 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      ) : meta.isPending ? (
        <div className="flex flex-1 items-center justify-center p-8">
          <Spinner />
        </div>
      ) : (
        <OrderStatistics meta={meta.data} />
      )}
    </PageShell>
  );
}

function OrderStatistics({ meta }: { meta: OrderStatisticsMeta }) {
  const t = useTranslations("fibu.auftrag");
  const state = useOrderStatisticsFilter(meta);
  const [tab, setTab] = useTabParam(
    "forecast",
    meta.contributionMargin ? ["contributionMargin"] : []
  );
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="px-4 pt-2">
        <OrderStatisticsFilterBar
          elements={meta.filterElements}
          state={state}
        />
      </div>
      <Tabs
        value={tab}
        onValueChange={setTab}
        className="flex min-h-0 flex-1 flex-col"
      >
        <TabsList className="mx-4 mt-2 w-fit shrink-0">
          <TabsTrigger value="forecast">{t("forecast._")}</TabsTrigger>
          {meta.contributionMargin && (
            <TabsTrigger value="contributionMargin">
              {t("contributionMargin._")}
            </TabsTrigger>
          )}
        </TabsList>
        <TabsContent value="forecast" className="min-h-0 overflow-auto">
          <OrderForecastChartsView
            filter={state.filter}
            planningDateHint={meta.planningDateHint}
          />
        </TabsContent>
        {meta.contributionMargin && (
          <TabsContent
            value="contributionMargin"
            className="min-h-0 overflow-auto"
          >
            <OrderContributionMarginView filter={state.filter} />
          </TabsContent>
        )}
      </Tabs>
    </div>
  );
}
