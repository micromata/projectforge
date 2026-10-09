"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  FillLevelBar,
  fillLevelPercent,
} from "@/components/shared/fill-level-bar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TabsContent } from "@/components/ui/tabs";
import {
  fetchSystemStatisticsSection,
  fetchSystemStatisticsSections,
} from "@/lib/rs/system-statistics";
import type { SystemStatisticsEntry } from "./system-statistics-types";

export const SYSTEM_STATISTICS_QUERY_KEY = "systemStatistics";

/**
 * The statistics tab of the system dashboard (`?tab=statistics`), successor of the dynamic React page. Every
 * section (one per statistics builder of the backend) is fetched on its own, so each card appears as soon as its
 * data is there instead of the tab waiting for the slowest one (gc, disk usage). Visible for all users; the backend
 * gives non-admins only the key figures of the database.
 */
export function SystemStatisticsTab() {
  return (
    <TabsContent value="statistics">
      <StatisticsSections />
    </TabsContent>
  );
}

/** Mounted with the open tab only, so the statistics are fetched once it is shown. */
function StatisticsSections() {
  const t = useTranslations();
  const sections = useQuery({
    queryKey: [SYSTEM_STATISTICS_QUERY_KEY, "sections"],
    queryFn: ({ signal }) => fetchSystemStatisticsSections(signal),
  });
  return (
    <div className="gap-4 pb-4 pt-2 lg:columns-2 [&>*]:mb-4 [&>*]:break-inside-avoid">
      {sections.isPending && <SectionSkeleton />}
      {sections.isError && (
        <p className="text-sm text-destructive">{t("errorpage.title")}</p>
      )}
      {sections.data?.map((section) => (
        <StatisticsSection key={section.id} id={section.id} />
      ))}
    </div>
  );
}

/** One section: one card per group of its entries, a placeholder card while it is loading. */
function StatisticsSection({ id }: { id: string }) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: [SYSTEM_STATISTICS_QUERY_KEY, "section", id],
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
      {/* Set off from the figures, so the groups are told apart at a glance in the floating columns. */}
      <CardHeader className="border-b">
        <CardTitle className="text-primary">{group}</CardTitle>
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
      <CardHeader className="border-b">
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
