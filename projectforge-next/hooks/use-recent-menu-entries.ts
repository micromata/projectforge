"use client";

import { useMenu } from "@/hooks/use-menu";
import { useReportMenuUsage } from "@/hooks/use-report-menu-usage";

/**
 * The menu entries the user opened last, most recent first — the quick access palette's head start.
 *
 * The history lives in the backend, not in this browser: most menu entries still point at Wicket or
 * at the legacy React app, so an entry the user opens there would otherwise never show up here, and
 * a second browser would start over. The server keeps it per user and resolves it against the menu
 * it just built (RecentMenuEntriesService), so entries are access-filtered and titled for free.
 *
 * Urls, not `MenuItem.key`, so the caller can match them against MenuEntry.key — see the note there
 * on why the url is the identity of an entry.
 */
export function useRecentMenuEntries(): {
  recentKeys: string[];
  remember: (key: string | undefined) => void;
} {
  const { data: menu } = useMenu();
  const remember = useReportMenuUsage();
  const recentKeys =
    menu?.recentMenu?.menuItems
      ?.map((item) => item.url)
      .filter((url): url is string => !!url) ?? EMPTY;

  return { recentKeys, remember };
}

const EMPTY: string[] = [];
