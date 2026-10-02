"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { OrderForecastChartsView } from "@/components/features/order/forecast-chart/order-forecast-charts-view";
import { OrderContributionMarginView } from "@/components/features/order/contribution-margin/order-contribution-margin-view";
import {
  ORDER_ENTITY,
  ORDER_PAGE,
} from "@/components/features/order/order.page";
import { useListMeta } from "@/hooks/use-list-meta";

/**
 * The order book (`/order`). Tabs under one page shell, as the invoice page composes its list and
 * charts: the order list, a "Grafiken" tab with the forecast charts of the filtered orders and, for finance,
 * controlling and project managers (list meta variable `contributionMargin`), a "Deckungsbeitrag" tab with
 * the contribution margin of their projects. The list keeps its full chrome by rendering
 * {@link EntityListPage} `embedded`; the other tabs read the same filter the list is showing.
 */
export default function OrderListPage() {
  const t = useTranslations("fibu.auftrag");
  const listMeta = useListMeta(ORDER_ENTITY);
  const contributionMargin =
    listMeta.data?.variables?.contributionMargin === true;
  return (
    <PageShell>
      <Tabs defaultValue="list" className="flex min-h-0 flex-1 flex-col">
        <TabsList className="mx-4 mt-2 w-fit shrink-0">
          <TabsTrigger value="list">{t("title.list")}</TabsTrigger>
          <TabsTrigger value="charts">{t("forecast.chart._")}</TabsTrigger>
          {contributionMargin && (
            <TabsTrigger value="contributionMargin">
              {t("contributionMargin._")}
            </TabsTrigger>
          )}
        </TabsList>
        <TabsContent value="list" className="flex min-h-0 flex-col">
          <EntityListPage page={ORDER_PAGE} embedded />
        </TabsContent>
        <TabsContent value="charts" className="min-h-0 overflow-auto">
          <OrderForecastChartsView />
        </TabsContent>
        {contributionMargin && (
          <TabsContent
            value="contributionMargin"
            className="min-h-0 overflow-auto"
          >
            <OrderContributionMarginView />
          </TabsContent>
        )}
      </Tabs>
    </PageShell>
  );
}
