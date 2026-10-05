"use client";

import { useMemo, useState } from "react";
import {
  filterEntriesOf,
  filterFingerprint,
  filterValuesFromEntries,
  type FilterValues,
} from "@/components/data-table/filter-value";
import { toast } from "@/lib/toast";
import {
  createOrderStatisticsFavorite,
  deleteOrderStatisticsFavorite,
  renameOrderStatisticsFavorite,
  selectOrderStatisticsFavorite,
  updateOrderStatisticsFavorite,
  type OrderStatisticsFavoritesResponse,
  type OrderStatisticsMeta,
} from "@/lib/rs/order-statistics";
import type { FavoriteIdTitle, MagicFilter } from "@/lib/rs/types";

/** The favorite the filter on screen came from: its id and name travel with every chart request. */
interface FavoriteRef {
  id?: number | null;
  name?: string | null;
  /** {@link filterFingerprint} of the favorite as saved; null without a favorite. */
  baseline: string | null;
}

function refOf(
  filter: MagicFilter,
  favorite: MagicFilter | null | undefined
): FavoriteRef {
  return {
    id: filter.id,
    name: filter.name,
    baseline:
      filter.id != null && favorite ? filterFingerprint(favorite) : null,
  };
}

/**
 * The filter of the order statistics page (business units, customers, projects) and its favorites.
 *
 * The values live here, seeded once from the stored filter of {@link OrderStatisticsMeta}; the backend
 * stores them as the current filter with every chart request, so there is no save button. The favorites
 * calls answer with the new current filter, which then replaces the values on screen.
 */
export function useOrderStatisticsFilter(meta: OrderStatisticsMeta) {
  const [values, setValues] = useState<FilterValues>(() =>
    filterValuesFromEntries(meta.filter.entries)
  );
  const [ref, setRef] = useState<FavoriteRef>(() =>
    refOf(meta.filter, meta.favorite)
  );
  const [favorites, setFavorites] = useState<FavoriteIdTitle[]>(
    meta.filterFavorites
  );

  const filter = useMemo<MagicFilter>(
    () => ({
      entries: filterEntriesOf(values),
      sortProperties: [],
      id: ref.id ?? undefined,
      name: ref.name ?? undefined,
    }),
    [values, ref.id, ref.name]
  );
  const isModified =
    ref.baseline != null && filterFingerprint(filter) !== ref.baseline;

  /** Takes over the answer; `asFavorite`: the answer's filter is the favorite as saved (new baseline). */
  const run = async (
    call: Promise<OrderStatisticsFavoritesResponse>,
    asFavorite: boolean
  ) => {
    try {
      const response = await call;
      setFavorites(response.filterFavorites);
      if (asFavorite) {
        setValues(filterValuesFromEntries(response.filter.entries));
        setRef(refOf(response.filter, response.filter));
      } else {
        // Rename or delete: the criteria on screen stay, only the reference may change.
        setRef((prev) =>
          response.filter.id == null
            ? { baseline: null }
            : { ...prev, id: response.filter.id, name: response.filter.name }
        );
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    }
  };

  return {
    values,
    setValues,
    filter,
    favorites,
    currentId: ref.id ?? null,
    isModified,
    select: (id: number) => run(selectOrderStatisticsFavorite(id), true),
    create: (name: string) =>
      run(
        createOrderStatisticsFavorite({ ...filter, id: undefined, name }),
        true
      ),
    update: (id: number) =>
      run(updateOrderStatisticsFavorite({ ...filter, id }), true),
    rename: (id: number, newName: string) =>
      run(renameOrderStatisticsFavorite(id, newName), false),
    remove: (id: number) => run(deleteOrderStatisticsFavorite(id), false),
  };
}
