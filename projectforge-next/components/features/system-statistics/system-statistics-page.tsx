"use client";

import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  FillLevelBar,
  fillLevelPercent,
} from "@/components/shared/fill-level-bar";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  fetchSystemStatisticsSection,
  fetchSystemStatisticsSections,
} from "@/lib/rs/system-statistics";
import type { SystemStatisticsEntry } from "./types";

const QUERY_KEY = "systemStatistics";

/**
 * The system statistics page ("/next/systemStatistics"), successor of the dynamic React page. Every
 * section (one per statistics builder of the backend) is fetched on its own, so each card appears as
 * soon as its data is there instead of the page waiting for the slowest one (gc, disk usage). Visible
 * for all users; the backend gives non-admins only the key figures of the database.
 */
export function SystemStatisticsPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const sections = useQuery({
    queryKey: [QUERY_KEY, "sections"],
    queryFn: ({ signal }) => fetchSystemStatisticsSections(signal),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.systemStatistics")}
        title={t("system.statistics.title")}
      >
        <Button
          size="sm"
          variant="outline"
          disabled={!sections.data}
          onClick={() =>
            queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
          }
        >
          {t("refresh")}
        </Button>
      </PageTitleRow>
      <div className="grid items-start gap-4 px-4 pb-8 pt-2 xl:grid-cols-2">
        {sections.isPending && <SectionSkeleton />}
        {sections.isError && (
          <p className="text-sm text-destructive">{t("errorpage.title")}</p>
        )}
        {sections.data?.map((section) => (
          <StatisticsSection key={section.id} id={section.id} />
        ))}
      </div>
    </PageShell>
  );
}

/** One section: one card per group of its entries, a placeholder card while it is loading. */
function StatisticsSection({ id }: { id: string }) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: [QUERY_KEY, "section", id],
    queryFn: ({ signal }) => fetchSystemStatisticsSection(id, signal),
  });
  if (query.isPending) return <SectionSkeleton />;
  if (query.isError) {
    return (
      <Card>
        <CardContent>
          <p className="text-sm text-destructive">{t("errorpage.title")}</p>
        </CardContent>
      </Card>
    );
  }
  const groups = new Map<string, SystemStatisticsEntry[]>();
  query.data.entries.forEach((entry) => {
    const list = groups.get(entry.group);
    if (list) list.push(entry);
    else groups.set(entry.group, [entry]);
  });
  // A section without entries (e.g. no sync running yet) shows nothing.
  return [...groups.entries()].map(([group, entries]) => (
    <Card key={group}>
      <CardHeader>
        <CardTitle>{group}</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid grid-cols-1 gap-x-4 gap-y-2 text-sm sm:grid-cols-[minmax(10rem,1fr)_2fr]">
          {entries.map((entry) => (
            <StatisticsRow key={entry.id} entry={entry} />
          ))}
        </dl>
      </CardContent>
    </Card>
  ));
}

function StatisticsRow({ entry }: { entry: SystemStatisticsEntry }) {
  const gauge = entry.gauge;
  return (
    <>
      <dt className="text-muted-foreground">{entry.title}</dt>
      <dd className="min-w-0 break-words">
        {entry.value}
        {gauge && gauge.max > 0 && (
          <div className="mt-1 flex items-center gap-2">
            <FillLevelBar
              used={gauge.used}
              max={gauge.max}
              label={entry.title}
              className="flex-1"
            />
            <span className="w-10 text-right text-xs tabular-nums text-muted-foreground">
              {Math.round(fillLevelPercent(gauge.used, gauge.max))}%
            </span>
          </div>
        )}
      </dd>
    </>
  );
}

function SectionSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-4 w-32" />
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-5/6" />
        <Skeleton className="h-4 w-2/3" />
      </CardContent>
    </Card>
  );
}
