"use client";

import { useCallback, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useInitialList } from "@/hooks/use-initial-list";
import {
  createFilterFavorite,
  deleteFilterFavorite,
  renameFilterFavorite,
  selectFilterFavorite,
  updateFilterFavorite,
} from "@/lib/rs/client";
import type {
  FavoriteIdTitle,
  FilterFavoritesResponse,
  InitialListData,
  MagicFilter,
} from "@/lib/rs/types";

/**
 * The user's saved filters for one list, kept where the backend keeps them
 * (AbstractPagesRest `filter/*`, user prefs) — so they follow the user across
 * devices, and are the same favorites the legacy frontend shows.
 *
 * The list itself lives in the `initialList` query cache rather than in state of
 * its own: that is where it arrives, and every endpoint here returns the updated
 * list, so patching the cache keeps one source of truth.
 */
export interface UseFilterFavoritesOptions {
  entity: string;
  /** The filter the list is currently using; saved as-is when creating/updating. */
  filter: MagicFilter;
  /** Applies a saved filter to the page (values, search string, sorting). */
  onApply: (filter: MagicFilter) => void;
}

export interface UseFilterFavoritesResult {
  favorites: FavoriteIdTitle[];
  /** Id of the applied favorite, or undefined once the filter was edited since. */
  currentId: number | undefined;
  /** True while a request is in flight, so the UI can hold still. */
  isBusy: boolean;
  select: (id: number) => void;
  create: (name: string) => void;
  /** Overwrites the applied favorite with the current filter. */
  update: (id: number) => void;
  rename: (id: number, newName: string) => void;
  remove: (id: number) => void;
}

export function useFilterFavorites({
  entity,
  filter,
  onApply,
}: UseFilterFavoritesOptions): UseFilterFavoritesResult {
  const queryClient = useQueryClient();
  // Through the query, not queryClient.getQueryData: reading the cache directly
  // doesn't subscribe, so a renamed or deleted favorite wouldn't re-render.
  const layout = useInitialList(entity);

  // Which favorite is applied is client state, and deliberately starts empty: the
  // backend does keep an id on the stored current filter, but the page starts with
  // no filter values applied (restoring them is a separate step), so reading it
  // would name a favorite whose values are nowhere to be seen.
  const [currentId, setCurrentId] = useState<number>();

  const patchList = useCallback(
    (response: FilterFavoritesResponse) => {
      if (!response.filterFavorites) return;
      queryClient.setQueryData<InitialListData>(
        ["initialList", entity],
        (previous) =>
          previous
            ? { ...previous, filterFavorites: response.filterFavorites }
            : previous
      );
    },
    [queryClient, entity]
  );

  const selectMutation = useMutation({
    mutationFn: (id: number) => selectFilterFavorite(entity, id),
    onSuccess: (data, id) => {
      patchList(data);
      setCurrentId(id);
      if (data.filter) onApply(data.filter);
    },
  });

  const createMutation = useMutation({
    mutationFn: (name: string) =>
      // id must be unset — an id would make the backend treat this as an update.
      createFilterFavorite(entity, { ...filter, name, id: undefined }),
    onSuccess: (data) => {
      patchList(data);
      setCurrentId(data.filter?.id);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) =>
      updateFilterFavorite(entity, { ...filter, id, name }),
    // filter/update answers with an empty map, so there is nothing to patch: the
    // name and the list are unchanged, only the stored values are.
    onSuccess: (_data, { id }) => setCurrentId(id),
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, newName }: { id: number; newName: string }) =>
      renameFilterFavorite(entity, id, newName),
    onSuccess: patchList,
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => deleteFilterFavorite(entity, id),
    onSuccess: (data, id) => {
      patchList(data);
      // The filter values stay applied; only the saved copy is gone.
      if (currentId === id) setCurrentId(undefined);
    },
  });

  const favorites = layout.data?.filterFavorites ?? [];
  const nameOf = (id: number) => favorites.find((f) => f.id === id)?.name ?? "";

  return {
    favorites,
    currentId,
    isBusy:
      selectMutation.isPending ||
      createMutation.isPending ||
      updateMutation.isPending ||
      renameMutation.isPending ||
      removeMutation.isPending,
    select: selectMutation.mutate,
    create: createMutation.mutate,
    // The name has to travel with the values: filter/update replaces the whole
    // favorite, so leaving it out would clear the name it is listed under.
    update: (id) => updateMutation.mutate({ id, name: nameOf(id) }),
    rename: (id, newName) => renameMutation.mutate({ id, newName }),
    remove: removeMutation.mutate,
  };
}
