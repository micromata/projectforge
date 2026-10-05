"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
import { cn } from "@/lib/utils";
import {
  fetchContributionMarginDetails,
  type ContributionMarginData,
  type ContributionMarginSettings,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { StatisticsTableTabs } from "../statistics/statistics-table-tabs";
import { ContributionMarginInvoiceTable } from "./contribution-margin-invoice-table";
import { ContributionMarginMonthTable } from "./contribution-margin-month-table";
import { ContributionMarginPreliminaryLegend } from "./contribution-margin-preliminary";
import { ContributionMarginProjectTable } from "./contribution-margin-project-table";
import { ContributionMarginTimesheetTable } from "./contribution-margin-timesheet-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

/**
 * The project table of the contribution margin and, in further sub-tabs, the rows behind it (the sheets
 * Monats-DB, Rechnungen and DB-Zeitberichte of the DB Excel). The rows are asked for once `data` of the
 * same filter and start date is there (`enabled`): the backend has cached that calculation by then.
 *
 * A click on a row of the project table opens Monats-DB at the project's first month.
 */
export function ContributionMarginTables({
  data,
  filter,
  filterKey,
  params,
  enabled,
}: {
  data: ContributionMarginData;
  filter: MagicFilter;
  /** The stable derivation of `filter` the query key of `data` uses. */
  filterKey: string;
  params: ContributionMarginSettings;
  enabled: boolean;
}) {
  const t = useStatisticsLabels();
  const [tab, setTab] = useState("projects");
  /** The project Monats-DB was opened for from the project table; cleared by any other tab change. */
  const [focusProjectId, setFocusProjectId] = useState<number | null>(null);
  const query = useQuery({
    queryKey: ["order", "contributionMargin", "details", filterKey, params],
    queryFn: ({ signal }) =>
      fetchContributionMarginDetails(filter, params, signal),
    enabled,
    placeholderData: keepPreviousData,
  });
  const details = query.data;
  const pending = (
    <div className="flex items-center justify-center p-8">
      {query.isError ? (
        <p className="text-sm text-destructive">
          {query.error instanceof Error
            ? query.error.message
            : String(query.error)}
        </p>
      ) : (
        <Spinner />
      )}
    </div>
  );
  const dimmed = (content: React.ReactNode) => (
    <div
      className={cn(
        "space-y-2 transition-opacity",
        query.isPlaceholderData && "opacity-50"
      )}
      aria-busy={query.isPlaceholderData}
    >
      {content}
      <ContributionMarginPreliminaryLegend />
    </div>
  );
  return (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold">{t.heading}</h3>
      <StatisticsTableTabs
        value={tab}
        onValueChange={(value) => {
          setTab(value);
          setFocusProjectId(null);
        }}
        tabs={[
          {
            value: "projects",
            label: t.projects,
            count: data.projects.length,
            content: (
              <ContributionMarginProjectTable
                data={data}
                onProjectClick={(projectId) => {
                  setFocusProjectId(projectId);
                  setTab("months");
                }}
              />
            ),
          },
          {
            value: "months",
            label: t.months,
            count: details?.months.length,
            content: details
              ? dimmed(
                  <ContributionMarginMonthTable
                    rows={details.months}
                    data={data}
                    focusProjectId={focusProjectId}
                  />
                )
              : pending,
          },
          {
            value: "invoices",
            label: t.invoices,
            count: details?.invoices.length,
            content: details
              ? dimmed(
                  <ContributionMarginInvoiceTable rows={details.invoices} />
                )
              : pending,
          },
          {
            value: "timesheets",
            label: t.timesheets,
            count: details?.timesheets.length,
            content: details
              ? dimmed(
                  <ContributionMarginTimesheetTable rows={details.timesheets} />
                )
              : pending,
          },
        ]}
      />
    </section>
  );
}
