"use client";

import { useTranslations } from "next-intl";
import { ChartBarLineIcon } from "@hugeicons/core-free-icons";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ViewToggleButton } from "@/components/shared/list/view-toggle-button";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";
import { InvoiceChartsView } from "@/components/features/invoice/invoice-charts-view";
import { useTabParam } from "@/hooks/use-tab-param";

/**
 * The outgoing-invoice page (`/invoice`). Two views under one page shell, as the liquidity page composes its
 * list and forecast: the invoice list and a "Grafiken" view of the filtered invoices' monthly net sums over
 * four years. The list keeps its full chrome by rendering {@link EntityListPage} `embedded` — the shell is
 * this page's, shared with the charts view, which reads the same filter the list is showing. The views are
 * switched by a toggle button in the head rather than by tabs, so the page has the heading of every other
 * list. The open view lives in the url (`?tab=`, see useTabParam), so a reload stays on it.
 */
export default function InvoiceListPage() {
  const t = useTranslations("fibu.rechnung");
  const root = useTranslations();
  const [tab, setTab] = useTabParam("list", ["charts"]);
  const charts = tab === "charts";
  const toggle = (
    <ViewToggleButton
      icon={ChartBarLineIcon}
      label={t("chart._")}
      pressed={charts}
      onToggle={() => setTab(charts ? "list" : "charts")}
    />
  );
  return (
    <PageShell>
      {charts ? (
        <>
          <PageTitleRow category={root("menu.fibu._")} title={t("title.list")}>
            {toggle}
          </PageTitleRow>
          {/* min-h-64: on a short viewport the panel overflows <main>, so <main> scrolls to it; on a narrow or
              low screen it is as tall as <main> (see OrderStatistics). */}
          <div className="min-h-64 overflow-auto cramped:h-[100cqh] cramped:flex-none cramped:overflow-visible">
            <InvoiceChartsView />
          </div>
        </>
      ) : (
        <EntityListPage page={INVOICE_PAGE} embedded viewSwitch={toggle} />
      )}
    </PageShell>
  );
}
