"use client";

import { useTranslations } from "next-intl";
import { ChartBarLineIcon } from "@hugeicons/core-free-icons";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ViewToggleButton } from "@/components/shared/list/view-toggle-button";
import { LIQUIDITY_PAGE } from "@/components/features/liquidity/liquidity.page";
import { LiquidityForecastView } from "@/components/features/liquidity/liquidity-forecast-view";
import { useTabParam } from "@/hooks/use-tab-param";

/**
 * The liquidity-planning page (`/next/liquidity`), successor of Wicket's `LiquidityEntryListPage` and
 * `LiquidityForecastPage`. Two views under one page shell: the entry list and the "Liquiditätsvorschau"
 * forecast. The list keeps its full chrome by rendering {@link EntityListPage} `embedded` — the shell is
 * this page's, shared with the forecast view. A toggle button in the head switches between them (see
 * ViewToggleButton), so the page has the heading of every other list. The open view lives in the url
 * (`?tab=`, see useTabParam), so a reload stays on it.
 */
export default function LiquidityPage() {
  const t = useTranslations();
  const [tab, setTab] = useTabParam("entries", ["forecast"]);
  const forecast = tab === "forecast";
  const toggle = (
    <ViewToggleButton
      icon={ChartBarLineIcon}
      label={t("plugins.liquidityplanning.forecast._")}
      pressed={forecast}
      onToggle={() => setTab(forecast ? "entries" : "forecast")}
    />
  );
  return (
    <PageShell>
      {forecast ? (
        <>
          <PageTitleRow
            category={t("menu.reporting")}
            title={t("plugins.liquidityplanning.entry.title.list")}
          >
            {toggle}
          </PageTitleRow>
          {/* min-h-64: on a short viewport the panel overflows <main>, so <main> scrolls to it; on a narrow or
              low screen it is as tall as <main> (see OrderStatistics). */}
          <div className="min-h-64 overflow-auto cramped:h-[100cqh] cramped:flex-none cramped:overflow-visible">
            <LiquidityForecastView />
          </div>
        </>
      ) : (
        <EntityListPage page={LIQUIDITY_PAGE} embedded viewSwitch={toggle} />
      )}
    </PageShell>
  );
}
