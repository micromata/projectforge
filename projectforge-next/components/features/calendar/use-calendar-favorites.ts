"use client";

import { useMemo } from "react";
import { toast } from "@/lib/toast";
import {
  createCalendarFilter,
  deleteCalendarFilter,
  renameCalendarFilter,
  selectCalendarFilter,
  updateCalendarFilter,
} from "@/lib/rs/calendar";
import { useInitPatchRunner } from "./use-calendar-init";

/**
 * The saved calendar filters (the favourites menu). Create/update/rename/delete answer with a patch
 * that carries the new `filterFavorites`/`isFilterModified`; `select` re-initialises the page (its
 * `activeCalendars`, `date` and `view` all change), so it replaces the whole init and refetches events.
 */
export function useCalendarFavorites() {
  const { applyPatch, invalidateEvents, run } = useInitPatchRunner();

  return useMemo(
    () => ({
      create: (name: string) => run(createCalendarFilter(name)),
      update: (id: number) => run(updateCalendarFilter(id)),
      rename: (id: number, newName: string) =>
        run(renameCalendarFilter(id, newName)),
      remove: (id: number) => run(deleteCalendarFilter(id)),
      select: async (id: number) => {
        try {
          applyPatch(await selectCalendarFilter(id));
          await invalidateEvents();
        } catch (err) {
          toast.error(
            err instanceof Error ? err.message : "Could not apply the filter."
          );
        }
      },
    }),
    [run, applyPatch, invalidateEvents]
  );
}
