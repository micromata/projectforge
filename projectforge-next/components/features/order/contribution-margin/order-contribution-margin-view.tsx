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
import { cn } from "@/lib/utils";
import {
  fetchContributionMargin,
  fetchContributionMarginSettings,
  type ContributionMarginSettings,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { StatisticsDateField } from "../statistics/statistics-date-field";
import { ContributionMarginHints } from "./contribution-margin-hints";
import { ContributionMarginKpis } from "./contribution-margin-kpis";
import { ContributionMarginMonthlyChart } from "./contribution-margin-monthly-chart";
import { ContributionMarginTables } from "./contribution-margin-tables";

/** React Query key of the user's remembered start date (see fetchContributionMarginSettings). */
const CONTRIBUTION_MARGIN_SETTINGS_KEY = [
  "order",
  "contributionMargin",
  "settings",
];

/**
 * The "Deckungsbeitrag" tab of the order statistics (`OrderStatisticsPage`): the contribution margin (DB1)
 * of the projects of the orders `filter` selects, from the accounting records, completed for the months
 * not imported yet by the unbooked invoices and the time sheets (`ContributionMarginService`).
 *
 * `filter` is the page's own, as for the forecast charts (`OrderForecastChartsView`). Project managers see
 * only the projects they are responsible for; the backend drops the others.
 */
export function OrderContributionMarginView({
  filter,
}: {
  filter: MagicFilter;
}) {
  const settings = useQuery({
    queryKey: CONTRIBUTION_MARGIN_SETTINGS_KEY,
    queryFn: ({ signal }) => fetchContributionMarginSettings(signal),
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
  return <OrderContributionMargin filter={filter} settings={settings.data} />;
}

/**
 * The start date over the monthly chart, the project table and the rows behind it. Every change re-posts the request after a
 * short debounce; the backend persists the date with it, so there is no "apply" button.
 */
function OrderContributionMargin({
  filter,
  settings,
}: {
  filter: MagicFilter;
  settings: ContributionMarginSettings;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const tc = useTranslations();
  const [startDate, setStartDate] = useState<string | null>(
    settings.startDate ?? null
  );
  const params = useMemo<ContributionMarginSettings>(
    () => ({ startDate: startDate || null }),
    [startDate]
  );
  const debouncedParams = useDebouncedValue(params);
  const queryClient = useQueryClient();
  // The filter drives the query key, so a changed filter refetches.
  const filterKey = useMemo(() => JSON.stringify(filter), [filter]);
  const query = useQuery({
    queryKey: ["order", "contributionMargin", filterKey, debouncedParams],
    queryFn: async ({ signal }) => {
      const data = await fetchContributionMargin(
        filter,
        debouncedParams,
        signal
      );
      // The backend has just stored the date as the user's setting; the cached settings must follow (see
      // OrderForecastCharts).
      queryClient.setQueryData(
        CONTRIBUTION_MARGIN_SETTINGS_KEY,
        debouncedParams
      );
      return data;
    },
    staleTime: 0,
    placeholderData: keepPreviousData,
  });
  const recalculating =
    !query.isPending && (query.isFetching || params !== debouncedParams);

  const data = query.data;

  return (
    <div className="space-y-4 px-4 pb-4 pt-2">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        <StatisticsDateField
          id="contributionMarginStartDate"
          label={t("startDate._")}
          tooltip={t("startDate.tooltip")}
          value={startDate}
          onChange={setStartDate}
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
      </div>

      {data && <ContributionMarginHints data={data} />}

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
      ) : query.data.projects.length === 0 ? (
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
            id="order.contributionMargin"
            tiles={[
              {
                id: "kpis",
                title: t("kpis"),
                fixedHeight: true,
                render: () => <ContributionMarginKpis data={query.data} />,
              },
              {
                id: "monthly",
                title: t("monthly"),
                render: (className) => (
                  <ContributionMarginMonthlyChart
                    data={query.data}
                    className={className}
                  />
                ),
              },
            ]}
          />
          <ContributionMarginTables
            data={query.data}
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
