"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createTaskFavorite,
  deleteTaskFavorite,
  fetchTaskFavorites,
  renameTaskFavorite,
  selectTaskFavorite,
  type TaskFavorite,
} from "@/lib/rs/task";

/** Cache key — module-level so a write made anywhere refreshes the same list. */
const TASK_FAVORITES_KEY = ["task", "favorites"] as const;

/**
 * The user's saved task favorites (Strukturelementfavoriten) and the writes over them, kept where the
 * backend keeps them (`TaskFavoritesRest`, a per-user `UserPrefDO`) — so they follow the user across
 * devices and are the same favorites the legacy frontend shows.
 *
 * The list lives in the query cache as its single source of truth: create/rename/delete each answer
 * with the updated list, so patching the cache from the response keeps everything in step. Unlike the
 * list's filter favorites there is no "current/modified/overwrite" — a task favorite is just a stored
 * task, applied by resolving it to a task id (see [select]).
 */
export function useTaskFavorites() {
  const queryClient = useQueryClient();

  const { data } = useQuery<TaskFavorite[]>({
    queryKey: TASK_FAVORITES_KEY,
    queryFn: ({ signal }) => fetchTaskFavorites(signal),
    // A write refreshes it explicitly; between writes the list does not change under us.
    staleTime: Infinity,
  });

  const patchList = (list: TaskFavorite[]) =>
    queryClient.setQueryData(TASK_FAVORITES_KEY, list);

  const createMutation = useMutation({
    mutationFn: ({ name, taskId }: { name: string; taskId: number }) =>
      createTaskFavorite(name, taskId),
    onSuccess: patchList,
  });

  const selectMutation = useMutation({
    mutationFn: (id: number) => selectTaskFavorite(id),
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, newName }: { id: number; newName: string }) =>
      renameTaskFavorite(id, newName),
    onSuccess: patchList,
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => deleteTaskFavorite(id),
    onSuccess: patchList,
  });

  return {
    favorites: data ?? [],
    create: (name: string, taskId: number) =>
      createMutation.mutate({ name, taskId }),
    /** Resolves a favorite to its task id (or null), for the caller to load and apply. */
    select: (id: number) => selectMutation.mutateAsync(id),
    rename: (id: number, newName: string) =>
      renameMutation.mutate({ id, newName }),
    remove: (id: number) => removeMutation.mutate(id),
    isBusy:
      createMutation.isPending ||
      selectMutation.isPending ||
      renameMutation.isPending ||
      removeMutation.isPending,
  };
}
