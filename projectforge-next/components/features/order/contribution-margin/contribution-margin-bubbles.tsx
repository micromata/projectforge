"use client";

import { useMemo, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import {
  fetchContributionMarginBubbles,
  fetchContributionMarginBubbleSettings,
  type ContributionMarginBubbleSettings,
  type ContributionMarginSettings,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { ContributionMarginBubbleChart } from "./contribution-margin-bubble-chart";
import { ContributionMarginBubbleToolbar } from "./contribution-margin-bubble-toolbar";

const SETTINGS_KEY = ["order", "contributionMargin", "bubbles", "settings"];

/**
 * The bubble chart tile of the contribution margin tab with its grouping and the switch for the anonymous
 * other projects. It has a query of its own, so changing the grouping doesn't reload the rest of the tab; the
 * backend stores grouping and switch with each request.
 */
export function ContributionMarginBubbles({
  filter,
  params,
  className,
}: {
  filter: MagicFilter;
  params: ContributionMarginSettings;
  className?: string;
}) {
  const settings = useQuery({
    queryKey: SETTINGS_KEY,
    queryFn: ({ signal }) => fetchContributionMarginBubbleSettings(signal),
  });
  if (settings.isError) {
    return <ErrorText error={settings.error} />;
  }
  if (settings.isPending) {
    return <Spinner />;
  }
  return (
    <BubblesWithSettings
      filter={filter}
      params={params}
      initial={settings.data}
      className={className}
    />
  );
}

function BubblesWithSettings({
  filter,
  params,
  initial,
  className,
}: {
  filter: MagicFilter;
  params: ContributionMarginSettings;
  initial: ContributionMarginBubbleSettings;
  className?: string;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const [settings, setSettings] = useState(initial);
  const filterKey = useMemo(() => JSON.stringify(filter), [filter]);
  const query = useQuery({
    queryKey: [
      "order",
      "contributionMargin",
      "bubbles",
      filterKey,
      params,
      settings,
    ],
    queryFn: ({ signal }) =>
      fetchContributionMarginBubbles(
        filter,
        { ...params, ...settings },
        signal
      ),
    staleTime: 0,
    placeholderData: keepPreviousData,
  });
  return (
    <div className="space-y-2">
      <ContributionMarginBubbleToolbar
        value={settings}
        onChange={setSettings}
      />
      {query.isError ? (
        <ErrorText error={query.error} />
      ) : query.isPending ? (
        <Spinner />
      ) : query.data.bubbles.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("empty")}</p>
      ) : (
        <div
          className={cn(
            "transition-opacity",
            query.isPlaceholderData && "opacity-50"
          )}
          aria-busy={query.isFetching}
        >
          <ContributionMarginBubbleChart
            data={query.data}
            showProjects={settings.showProjects}
            className={className}
          />
        </div>
      )}
    </div>
  );
}

function ErrorText({ error }: { error: unknown }) {
  return (
    <p className="text-sm text-destructive">
      {error instanceof Error ? error.message : String(error)}
    </p>
  );
}
