"use client";

import { useCallback, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useListMeta } from "@/hooks/use-list-meta";
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
  ListMetaData,
  MagicFilter,
} from "@/lib/rs/types";
import { filterFingerprint } from "./filter-value";
import type { FavoriteRef } from "./use-list-filters";

/**
 * The user's saved filters for one list, kept where the backend keeps them
 * (AbstractEntityRest `filter/*`, user prefs) — so they follow the user across
 * devices, and are the same favorites the legacy frontend shows.
 *
 * The list itself lives in the `listMeta` query cache rather than in state of
 * its own: that is where it arrives, and every endpoint here returns the updated
 * list, so patching the cache keeps one source of truth.
 */
export interface UseFilterFavoritesOptions {
  entity: string;
  /** The filter the list is currently using; saved as-is when creating/updating. */
  filter: MagicFilter;
  /** Applies a saved filter to the page (values, search string, sorting). */
  onApply: (filter: MagicFilter) => void;
  /**
   * The favorite the current values are based on, owned by [useListFilters] because
   * the id has to travel with the filter the query sends. It stays set while the
   * user edits the values — that is what keeps "save into this favorite" reachable,
   * also after leaving the page and coming back.
   */
  current: FavoriteRef | undefined;
  onCurrentChange: (current: FavoriteRef | undefined) => void;
}

export interface UseFilterFavoritesResult {
  favorites: FavoriteIdTitle[];
  /** Id of the favorite the current values are based on. */
  currentId: number | undefined;
  /**
   * Whether the values differ from what that favorite has stored, i.e. whether
   * there is something to save. Unknown counts as modified — see the note on the
   * baseline below.
   */
  isModified: boolean;
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
  current,
  onCurrentChange,
}: UseFilterFavoritesOptions): UseFilterFavoritesResult {
  const queryClient = useQueryClient();
  // Through the query, not queryClient.getQueryData: reading the cache directly
  // doesn't subscribe, so a renamed or deleted favorite wouldn't re-render.
  const layout = useListMeta(entity);

  const currentId = current?.id;

  // What the favorite has stored, to tell "modified" from "up to date". Only known
  // for a favorite that was applied or written in this session: listMeta carries
  // the favorites' names, not their values (Favorites.idTitleList). Unknown means
  // modified, so saving stays reachable — the legacy frontend goes further and
  // hardcodes it (SearchFilter.jsx passes isModified unconditionally).
  const [savedPrint, setSavedPrint] = useState<
    { id: number; print: string } | undefined
  >();

  const patchList = useCallback(
    (response: FilterFavoritesResponse) => {
      if (!response.filterFavorites) return;
      queryClient.setQueryData<ListMetaData>(
        ["listMeta", entity],
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
      onCurrentChange({ id, name: data.filter?.name ?? nameOf(id) });
      if (data.filter) {
        // Just applied, so it is by definition unmodified.
        setSavedPrint({ id, print: filterFingerprint(data.filter) });
        onApply(data.filter);
      }
    },
  });

  const createMutation = useMutation({
    mutationFn: (name: string) =>
      // id must be unset — an id would make the backend treat this as an update.
      createFilterFavorite(entity, { ...filter, name, id: undefined }),
    onSuccess: (data) => {
      patchList(data);
      const id = data.filter?.id;
      if (id === undefined) return;
      onCurrentChange({ id, name: data.filter?.name });
      setSavedPrint({ id, print: filterFingerprint(filter) });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) =>
      updateFilterFavorite(entity, { ...filter, id, name }),
    // filter/update answers with an empty map, so there is nothing to patch: the
    // name and the list are unchanged, only the stored values are.
    onSuccess: (_data, { id, name }) => {
      onCurrentChange({ id, name });
      setSavedPrint({ id, print: filterFingerprint(filter) });
    },
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, newName }: { id: number; newName: string }) =>
      renameFilterFavorite(entity, id, newName),
    onSuccess: (data, { id, newName }) => {
      patchList(data);
      // The name travels with the filter, so the current reference has to follow.
      if (currentId === id) onCurrentChange({ id, name: newName });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => deleteFilterFavorite(entity, id),
    onSuccess: (data, id) => {
      patchList(data);
      // The filter values stay applied; only the saved copy is gone.
      if (currentId === id) onCurrentChange(undefined);
    },
  });

  const favorites = layout.data?.filterFavorites ?? [];
  const nameOf = (id: number) => favorites.find((f) => f.id === id)?.name ?? "";

  return {
    favorites,
    currentId,
    isModified: !(
      savedPrint &&
      savedPrint.id === currentId &&
      savedPrint.print === filterFingerprint(filter)
    ),
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
