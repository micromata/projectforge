"use client";

import { useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchRecentTasks,
  recordRecentTask,
  type TaskDisplayObject,
} from "@/lib/rs/task";

/** Cache key — module-level so a pick made anywhere can refresh the same list. */
const RECENT_TASKS_KEY = ["task", "recent"] as const;

/**
 * The tasks the user picked most recently in the task select element, and the recorder that keeps that
 * list current.
 *
 * Every commit of a task (see TaskSelectField and TaskSelect) calls [recordTask], which posts the pick
 * and seeds the cache with the fresh list the backend answers — the search popover then offers them as
 * quick-picks before anything is typed (see TaskSearchPopover). Recording is fire-and-forget: a failed
 * record must never block the pick it follows.
 */
export function useRecentTasks() {
  const queryClient = useQueryClient();

  const { data } = useQuery<TaskDisplayObject[]>({
    queryKey: RECENT_TASKS_KEY,
    queryFn: ({ signal }) => fetchRecentTasks(signal),
    // A pick refreshes it explicitly; between picks the list does not change under us.
    staleTime: Infinity,
  });

  const recordTask = useCallback(
    (id: number) => {
      recordRecentTask(id)
        .then((list) => queryClient.setQueryData(RECENT_TASKS_KEY, list))
        .catch(() => {
          // Recording is a convenience, not part of the pick — swallow and leave the list as it was.
        });
    },
    [queryClient]
  );

  return { recentTasks: data ?? [], recordTask };
}
