"use client";

import { useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  createTimesheetFavorite,
  deleteTimesheetFavorite,
  fetchRecentTimesheets,
  fetchTimesheetFavorites,
  renameTimesheetFavorite,
  selectRecentTimesheet,
  selectTimesheetFavorite,
  type RecentTimesheets,
  type TimesheetFavorite,
} from "@/lib/rs/timesheet";
import type { TimesheetDetail } from "./types";

/** Cache keys — module-level so a save elsewhere could invalidate them by the same key. */
const RECENT_KEY = ["timesheet", "recent"] as const;
const FAVORITES_KEY = ["timesheet", "favorites"] as const;

/**
 * The user's recent time sheets and saved templates, and the operations on them, bound to no form: the
 * caller says what the sheet on screen is (`current`) and how a template lands on it (`apply`).
 *
 * Both the recent select and the favorite select post the current sheet so the backend merges the entry
 * into it; the merged sheet it answers with is what is applied.
 */
export function useTimesheetTemplateList(
  current: () => TimesheetDetail,
  apply: (template: TimesheetDetail) => void
) {
  const queryClient = useQueryClient();

  const recent = useQuery<RecentTimesheets>({
    queryKey: RECENT_KEY,
    queryFn: ({ signal }) => fetchRecentTimesheets(signal),
    staleTime: Infinity,
  });
  const favorites = useQuery<TimesheetFavorite[]>({
    queryKey: FAVORITES_KEY,
    queryFn: ({ signal }) => fetchTimesheetFavorites(signal),
    staleTime: Infinity,
  });

  const applyRecent = useCallback(
    async (entry: TimesheetDetail) => {
      try {
        const merged = await selectRecentTimesheet({ ...current(), ...entry });
        apply(merged ?? entry);
      } catch (err) {
        toast.error(err instanceof Error ? err.message : String(err));
      }
    },
    [apply, current]
  );

  const applyFavorite = useCallback(
    async (id: number) => {
      try {
        const merged = await selectTimesheetFavorite(id, current());
        if (merged) apply(merged);
      } catch (err) {
        toast.error(err instanceof Error ? err.message : String(err));
      }
    },
    [apply, current]
  );

  /** Runs a write that answers with the new favorites list and refreshes the cache from it. */
  const write = useCallback(
    async (op: () => Promise<TimesheetFavorite[]>) => {
      try {
        queryClient.setQueryData(FAVORITES_KEY, await op());
      } catch (err) {
        toast.error(err instanceof Error ? err.message : String(err));
      }
    },
    [queryClient]
  );

  return {
    recent: recent.data,
    favorites: favorites.data ?? [],
    applyRecent,
    applyFavorite,
    create: (name: string) =>
      write(() => createTimesheetFavorite(name, current())),
    rename: (id: number, newName: string) =>
      write(() => renameTimesheetFavorite(id, newName)),
    remove: (id: number) => write(() => deleteTimesheetFavorite(id)),
  };
}

/** What the template list hands out — the props the templates bar is built from. */
export type TimesheetTemplateList = ReturnType<typeof useTimesheetTemplateList>;
