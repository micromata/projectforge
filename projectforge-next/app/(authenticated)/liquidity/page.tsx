"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LIQUIDITY_PAGE } from "@/components/features/liquidity/liquidity.page";
import { LiquidityForecastView } from "@/components/features/liquidity/liquidity-forecast-view";

/**
 * The liquidity-planning page (`/next/liquidity`), successor of Wicket's `LiquidityEntryListPage` and
 * `LiquidityForecastPage`. Two tabs under one page shell: the entry list and the "Liquiditätsvorschau"
 * forecast. The list keeps its full chrome by rendering {@link EntityListPage} `embedded` — the shell is
 * this page's, shared with the forecast tab.
 */
export default function LiquidityPage() {
  const t = useTranslations();
  return (
    <PageShell>
      <Tabs defaultValue="entries" className="flex min-h-0 flex-1 flex-col">
        <TabsList className="mx-4 mt-2 w-fit shrink-0">
          <TabsTrigger value="entries">
            {t("plugins.liquidityplanning.entry.title.list")}
          </TabsTrigger>
          <TabsTrigger value="forecast">
            {t("plugins.liquidityplanning.forecast._")}
          </TabsTrigger>
        </TabsList>
        <TabsContent value="entries" className="flex min-h-0 flex-col">
          <EntityListPage page={LIQUIDITY_PAGE} embedded />
        </TabsContent>
        <TabsContent value="forecast" className="min-h-0 overflow-auto">
          <LiquidityForecastView />
        </TabsContent>
      </Tabs>
    </PageShell>
  );
}
