"use client";

import { useMemo, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRememberedFilter } from "@/components/data-table/use-remembered-filter";
import { DateInput } from "@/components/shared/date-input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Label } from "@/components/ui/label";
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
import { ORDER_ENTITY } from "../order.page";
import { OrderForecastCumulativeChart } from "./order-forecast-cumulative-chart";
import { OrderForecastMonthlyChart } from "./order-forecast-monthly-chart";

/**
 * The "Grafiken" tab of `/order` (see `app/(authenticated)/order/page.tsx`): the charts of the forecast
 * Excel export (sheet 'Grafiken 1'), computed by the very export pipeline (`ForecastExport.chartData`), so
 * the values match the Excel of the same filter, start and planning date.
 *
 * Follows the filter the list is showing through {@link useRememberedFilter}; the period of performance of
 * that filter is replaced by the start date of the controls. Start and planning date are remembered by the
 * backend, so the settings are loaded first.
 */
export function OrderForecastChartsView() {
  const remembered = useRememberedFilter(ORDER_ENTITY);
  const settings = useQuery({
    queryKey: ["order", "forecastChart", "settings"],
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
  if (remembered.isPending || settings.isPending) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  return (
    <OrderForecastCharts filter={remembered.filter} settings={settings.data} />
  );
}

/**
 * The controls (start date, optional planning date) over the two charts. Every change re-posts the request
 * after a short debounce; the backend persists the dates with it, so there is no "apply" button.
 */
function OrderForecastCharts({
  filter,
  settings,
}: {
  filter: MagicFilter | undefined;
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
  const params = useMemo<ForecastChartSettings>(
    () => ({
      startDate: startDate || null,
      planningDate: planningDate || null,
    }),
    [startDate, planningDate]
  );
  const debouncedParams = useDebouncedValue(params);
  // The filter drives the query key, so a changed list filter refetches when the user returns to this tab.
  const filterKey = useMemo(() => JSON.stringify(filter ?? {}), [filter]);
  const query = useQuery({
    queryKey: ["order", "forecastChart", filterKey, debouncedParams],
    queryFn: ({ signal }) =>
      fetchForecastChart(
        filter ?? { entries: [], sortProperties: [] },
        debouncedParams,
        signal
      ),
    placeholderData: keepPreviousData,
  });
  // Recalculating: a request is running, or a changed date is still waiting for the debounce. The previous
  // charts stay visible (keepPreviousData), but dimmed, so a date change doesn't look like it had no effect.
  const recalculating =
    !query.isPending && (query.isFetching || params !== debouncedParams);

  return (
    <div className="space-y-6 p-4">
      {/* The charts skip the detail of the Excel (e.g. a chosen variant, snapshots, the project overview). */}
      <Alert>
        <AlertDescription>{t("quickViewHint")}</AlertDescription>
      </Alert>
      <div className="flex flex-wrap items-end gap-4">
        <div className="grid gap-1.5">
          <HintTooltip text={t("startDate.tooltip")} openOnTap>
            <Label htmlFor="forecastStartDate">{t("startDate._")}</Label>
          </HintTooltip>
          <DateInput
            id="forecastStartDate"
            value={startDate}
            onChange={setStartDate}
            aria-label={t("startDate._")}
          />
        </div>
        <div className="grid gap-1.5">
          <HintTooltip text={t("planningDate.tooltip")} openOnTap>
            <Label htmlFor="forecastPlanningDate">{t("planningDate._")}</Label>
          </HintTooltip>
          <DateInput
            id="forecastPlanningDate"
            value={planningDate}
            onChange={setPlanningDate}
            aria-label={t("planningDate._")}
          />
        </div>
        {recalculating && (
          <p
            className="flex items-center gap-2 pb-2 text-sm text-muted-foreground"
            role="status"
          >
            <Spinner className="h-4 w-4 border-2" />
            {tc("loading")}
          </p>
        )}
        {!recalculating && query.data?.plan && query.data.planningDate && (
          <p className="pb-2 text-sm text-muted-foreground">
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
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{t("monthly")}</h3>
            <OrderForecastMonthlyChart data={query.data} />
          </section>
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{t("cumulative")}</h3>
            <OrderForecastCumulativeChart data={query.data} />
          </section>
        </div>
      )}
    </div>
  );
}
