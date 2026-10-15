"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";
import { InvoiceChartsView } from "@/components/features/invoice/invoice-charts-view";
import { useTabParam } from "@/hooks/use-tab-param";

/**
 * The outgoing-invoice page (`/invoice`). Two tabs under one page shell, as the liquidity page composes its
 * list and forecast: the invoice list and a "Grafiken" tab of the filtered invoices' monthly net sums over
 * four years. The list keeps its full chrome by rendering {@link EntityListPage} `embedded` — the shell is
 * this page's, shared with the charts tab, which reads the same filter the list is showing. The open tab
 * lives in the url (`?tab=`, see useTabParam), so a reload stays on it.
 */
export default function InvoiceListPage() {
  const t = useTranslations("fibu.rechnung");
  const [tab, setTab] = useTabParam("list", ["charts"]);
  return (
    <PageShell>
      <Tabs
        value={tab}
        onValueChange={setTab}
        className="flex min-h-0 flex-1 flex-col"
      >
        <TabsList className="mx-4 mt-2 w-fit shrink-0">
          <TabsTrigger value="list">{t("title.list")}</TabsTrigger>
          <TabsTrigger value="charts">{t("chart._")}</TabsTrigger>
        </TabsList>
        <TabsContent value="list" className="flex min-h-0 flex-col">
          <EntityListPage page={INVOICE_PAGE} embedded />
        </TabsContent>
        <TabsContent value="charts" className="min-h-0 overflow-auto">
          <InvoiceChartsView />
        </TabsContent>
      </Tabs>
    </PageShell>
  );
}
