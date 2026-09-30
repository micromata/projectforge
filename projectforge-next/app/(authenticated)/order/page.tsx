"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ORDER_PAGE } from "@/components/features/order/order.page";
import { OrderForecastChartsView } from "@/components/features/order/forecast-chart/order-forecast-charts-view";

/**
 * The order book (`/order`). Two tabs under one page shell, as the invoice page composes its list and
 * charts: the order list and a "Grafiken" tab with the forecast charts of the filtered orders. The list
 * keeps its full chrome by rendering {@link EntityListPage} `embedded`; the charts tab reads the same filter
 * the list is showing.
 */
export default function OrderListPage() {
  const t = useTranslations("fibu.auftrag");
  return (
    <PageShell>
      <Tabs defaultValue="list" className="flex min-h-0 flex-1 flex-col">
        <TabsList className="mx-4 mt-2 w-fit shrink-0">
          <TabsTrigger value="list">{t("title.list")}</TabsTrigger>
          <TabsTrigger value="charts">{t("forecast.chart._")}</TabsTrigger>
        </TabsList>
        <TabsContent value="list" className="flex min-h-0 flex-col">
          <EntityListPage page={ORDER_PAGE} embedded />
        </TabsContent>
        <TabsContent value="charts" className="min-h-0 overflow-auto">
          <OrderForecastChartsView />
        </TabsContent>
      </Tabs>
    </PageShell>
  );
}
