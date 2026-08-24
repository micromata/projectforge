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
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { TimesheetDetail } from "../types";
import type { TimesheetEditValues } from "./timesheet-edit-schema";
import { templateFieldsOf, toTimesheetDetail } from "./timesheet-edit-values";

/** Cache keys — module-level so a save elsewhere could invalidate them by the same key. */
const RECENT_KEY = ["timesheet", "recent"] as const;
const FAVORITES_KEY = ["timesheet", "favorites"] as const;

/**
 * The recent entries and saved templates of the time sheet form, and everything that fills the form
 * from one of them.
 *
 * A template — a recent sheet or a named favorite — carries the *what* of a booking and not its *when*
 * (see templateFieldsOf), so applying one leaves the period the user set. Both the recent select and the
 * favorite select post the sheet on screen so the backend merges the entry into it; the merged sheet it
 * answers with is what lands on the form.
 */
export function useTimesheetTemplates() {
  const form = useEntityEditForm();
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

  /** Merges a template's fields over the form, keeping the period and identity of the sheet. */
  const apply = useCallback(
    (template: TimesheetDetail | null) => {
      if (!template) return;
      const fields = templateFieldsOf(template);
      for (const [name, value] of Object.entries(fields)) {
        form.setFieldValue(name, value);
      }
    },
    [form]
  );

  /** What the backend merges a template into: the sheet as it stands on the form. */
  const current = useCallback(
    (): TimesheetDetail =>
      toTimesheetDetail(form.state.values as TimesheetEditValues),
    [form]
  );

  const applyRecent = useCallback(
    async (entry: TimesheetDetail) => {
      try {
        const result = await selectRecentTimesheet({ ...current(), ...entry });
        if (result.kind === "ok") {
          apply((result.action.variables?.data as TimesheetDetail) ?? entry);
        } else if (result.kind === "rejected") {
          toast.error(result.message);
        }
      } catch (err) {
        toast.error(err instanceof Error ? err.message : String(err));
      }
    },
    [apply, current]
  );

  const applyFavorite = useCallback(
    async (id: number) => {
      try {
        apply(await selectTimesheetFavorite(id, current()));
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
