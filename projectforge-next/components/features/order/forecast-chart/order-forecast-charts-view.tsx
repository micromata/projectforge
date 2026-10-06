"use client";

import { useMemo, useState } from "react";
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChartDashboard } from "@/components/shared/dashboard/chart-dashboard";
import { Spinner } from "@/components/shared/spinner";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";
import {
  fetchForecastChart,
  fetchForecastChartSettings,
  type ForecastChartSettings,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { OrderForecastCumulativeChart } from "./order-forecast-cumulative-chart";
import { OrderForecastMonthlyChart } from "./order-forecast-monthly-chart";
import { StatisticsDateField } from "../statistics/statistics-date-field";
import { ForecastTables } from "./forecast-tables";
import { ForecastVariantField } from "./forecast-variant-field";

/** React Query key of the user's remembered chart parameters (see fetchForecastChartSettings). */
const FORECAST_CHART_SETTINGS_KEY = ["order", "forecastChart", "settings"];

/**
 * The "Forecast" tab of the order statistics (`OrderStatisticsPage`): the charts of the forecast Excel
 * export (sheet 'Grafiken 1'), computed by the very export pipeline (`ForecastExport.chartData`), so the
 * values match the Excel of the same business units, customers, projects, start and planning date and
 * budget scenario.
 *
 * `filter` is the page's own (business units, customers, projects); the backend stores it with every
 * request, as it stores dates and scenario. Those parameters are remembered by the backend, so the
 * settings are loaded first.
 */
export function OrderForecastChartsView({ filter }: { filter: MagicFilter }) {
  const settings = useQuery({
    queryKey: FORECAST_CHART_SETTINGS_KEY,
    queryFn: ({ signal }) => fetchForecastChartSettings(signal),
  });
  if (settings.isError) {
    return (
      <p className="p-4 text-sm text-destructive">
        {settings.error instanceof Error
          ? settings.error.message
          : String(settings.error)}
      </p>
    );
  }
  if (settings.isPending) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  return <OrderForecastCharts filter={filter} settings={settings.data} />;
}

/**
 * The controls (start date, optional planning date, budget scenario) over the two charts and the tables
 * behind them. Every change re-posts the request after a short debounce; the backend persists the
 * parameters with it, so there is no "apply" button.
 */
function OrderForecastCharts({
  filter,
  settings,
}: {
  filter: MagicFilter;
  settings: ForecastChartSettings;
}) {
  const t = useTranslations("fibu.auftrag.forecast.chart");
  const tc = useTranslations();
  const ctx = useFormatContext();
  const [startDate, setStartDate] = useState<string | null>(
    settings.startDate ?? null
  );
  const [planningDate, setPlanningDate] = useState<string | null>(
    settings.planningDate ?? null
  );
  const [distributeUnusedBudget, setDistributeUnusedBudget] = useState(
    settings.distributeUnusedBudget
  );
  const params = useMemo<ForecastChartSettings>(
    () => ({
      startDate: startDate || null,
      planningDate: planningDate || null,
      distributeUnusedBudget,
    }),
    [startDate, planningDate, distributeUnusedBudget]
  );
  const debouncedParams = useDebouncedValue(params);
  const queryClient = useQueryClient();
  // The filter drives the query key, so a changed filter refetches.
  const filterKey = useMemo(() => JSON.stringify(filter), [filter]);
  const query = useQuery({
    queryKey: ["order", "forecastChart", filterKey, debouncedParams],
    queryFn: async ({ signal }) => {
      const data = await fetchForecastChart(filter, debouncedParams, signal);
      // The backend has just stored these dates as the user's settings; the cached settings must follow,
      // otherwise the tab re-seeds its controls with the dates of its first load when it is mounted again.
      queryClient.setQueryData(FORECAST_CHART_SETTINGS_KEY, debouncedParams);
      return data;
    },
    // Every request persists its dates, so returning to dates used a moment ago must post again rather
    // than answer from the cache — otherwise the backend keeps the dates in between.
    staleTime: 0,
    placeholderData: keepPreviousData,
  });
  // Recalculating: a request is running, or a changed date is still waiting for the debounce. The previous
  // charts stay visible (keepPreviousData), but dimmed, so a date change doesn't look like it had no effect.
  const recalculating =
    !query.isPending && (query.isFetching || params !== debouncedParams);

  return (
    <div className="space-y-4 px-4 pb-4 pt-2">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        <StatisticsDateField
          id="forecastStartDate"
          label={t("startDate._")}
          tooltip={t("startDate.tooltip")}
          value={startDate}
          onChange={setStartDate}
        />
        <StatisticsDateField
          id="forecastPlanningDate"
          label={t("planningDate._")}
          tooltip={t("planningDate.tooltip")}
          value={planningDate}
          onChange={setPlanningDate}
        />
        <ForecastVariantField
          value={distributeUnusedBudget}
          onChange={setDistributeUnusedBudget}
        />
        {recalculating && (
          <div
            className="flex items-center gap-2 text-sm text-muted-foreground"
            role="status"
          >
            <Spinner className="h-4 w-4 border-2" />
            {tc("loading")}
          </div>
        )}
        {!recalculating && query.data?.plan && query.data.planningDate && (
          <p className="text-sm text-muted-foreground">
            {t("planningDateUsed", {
              arg0: formatDate(query.data.planningDate, ctx),
            })}
          </p>
        )}
      </div>

      {query.isError ? (
        <p className="text-sm text-destructive">
          {query.error instanceof Error
            ? query.error.message
            : String(query.error)}
        </p>
      ) : query.isPending ? (
        <div className="flex items-center justify-center p-8">
          <Spinner />
        </div>
      ) : query.data.months.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("empty")}</p>
      ) : (
        <div
          className={cn(
            "space-y-8 transition-opacity",
            recalculating && "opacity-50"
          )}
          aria-busy={recalculating}
        >
          <ChartDashboard
            id="order.forecast"
            tiles={[
              {
                id: "monthly",
                title: t("monthly"),
                render: (className) => (
                  <OrderForecastMonthlyChart
                    data={query.data}
                    className={className}
                  />
                ),
              },
              {
                id: "cumulative",
                title: t("cumulative"),
                render: (className) => (
                  <OrderForecastCumulativeChart
                    data={query.data}
                    className={className}
                  />
                ),
              },
            ]}
          />
          <ForecastTables
            filter={filter}
            filterKey={filterKey}
            params={debouncedParams}
            enabled={query.isSuccess && !query.isPlaceholderData}
          />
        </div>
      )}
    </div>
  );
}
