"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { cn } from "@/lib/utils";
import {
  fetchContributionMarginBreakdown,
  type ContributionMarginLevel,
  type ContributionMarginSettings,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { ContributionMarginBreakdownHint } from "./contribution-margin-breakdown-hint";
import { ContributionMarginBreakdownTree } from "./contribution-margin-breakdown-tree";

/**
 * The contribution margin of the projects broken down by orders (and their positions) or by tasks
 * (Strukturelemente), as a tree: each project with its orders or task tree and its residual (what isn't
 * assignable). The values below the projects are calculated (see ContributionMarginService.calculateBreakdown),
 * as the hint above says. A click on a row shows its months in the chart above the table.
 */
export function ContributionMarginBreakdownView({
  level,
  filter,
  filterKey,
  params,
  enabled,
}: {
  level: ContributionMarginLevel;
  filter: MagicFilter;
  /** The stable derivation of `filter` the query key uses. */
  filterKey: string;
  params: ContributionMarginSettings;
  enabled: boolean;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const query = useQuery({
    queryKey: [
      "order",
      "contributionMargin",
      "breakdown",
      level,
      filterKey,
      params,
    ],
    queryFn: ({ signal }) =>
      fetchContributionMarginBreakdown(filter, params, level, signal),
    enabled,
    placeholderData: keepPreviousData,
  });
  if (query.isError) {
    return (
      <p className="p-4 text-sm text-destructive">
        {query.error instanceof Error
          ? query.error.message
          : String(query.error)}
      </p>
    );
  }
  const data = query.data;
  if (!data) {
    return (
      <div className="flex items-center justify-center p-8">
        <Spinner />
      </div>
    );
  }
  return (
    <div
      className={cn(
        "space-y-4 transition-opacity",
        query.isPlaceholderData && "opacity-50"
      )}
      aria-busy={query.isPlaceholderData}
    >
      <ContributionMarginBreakdownHint hourlyRate={data.hourlyRate} />
      {data.rows.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("breakdown.empty")}</p>
      ) : (
        // Keyed by the rows, so expanded rows and selection start anew with another result.
        <ContributionMarginBreakdownTree
          key={`${data.level}-${data.rows.length}`}
          data={data}
        />
      )}
    </div>
  );
}
