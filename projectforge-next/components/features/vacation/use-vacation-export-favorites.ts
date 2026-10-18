"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  createVacationExportFavorite,
  deleteVacationExportFavorite,
  fetchVacationExportInitial,
  renameVacationExportFavorite,
  selectVacationExportFavorite,
  updateVacationExportFavorite,
} from "@/lib/rs/vacation-export";
import { toast } from "@/lib/toast";
import type { VacationExportFavorite, VacationExportRef } from "./types";
import {
  favoriteIdHolding,
  sameChoice,
} from "./vacation-export-favorites-model";

/**
 * The favorites of the team vacation calendar: named choices of users and groups (stored by the backend with
 * the user's choice, `VacationExportRest`), bound to the presentational [FavoritesMenu].
 *
 * The favorite applied last is the current one; it reads as modified as soon as the users or groups on screen
 * differ from it (as sets: the order means nothing). Applying one takes only what the user may (still) pick; the
 * backend answers the favorites with just those ids, so a favorite of a since deactivated colleague doesn't read
 * as modified right after applying it. A favorite gone meanwhile (deleted in another tab) or with nothing left to
 * pick changes nothing on screen: a notice says so, and the list is fetched anew.
 */
export function useVacationExportFavorites({
  initial,
  users,
  groups,
  apply,
}: {
  initial: VacationExportFavorite[];
  users: EntityRef[];
  groups: EntityRef[];
  apply: (users: VacationExportRef[], groups: VacationExportRef[]) => void;
}) {
  const t = useTranslations();
  const [favorites, setFavorites] = useState(initial);
  const choice = {
    userIds: users.map((e) => e.id),
    groupIds: groups.map((g) => g.id),
  };
  // Without one applied yet, the favorite holding exactly the choice on screen (e.g. the stored one).
  const [currentId, setCurrentId] = useState<number | null>(() =>
    favoriteIdHolding(initial, choice)
  );
  const current = favorites.find((f) => f.id === currentId) ?? null;
  const isModified = current != null && !sameChoice(current, choice);

  const run = async <T>(call: () => Promise<T>): Promise<T | undefined> => {
    try {
      return await call();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
      return undefined;
    }
  };

  return {
    favorites,
    currentId,
    isModified,
    select: async (id: number) => {
      const selection = await run(() => selectVacationExportFavorite(id));
      if (!selection) return;
      if (selection.users.length === 0 && selection.groups.length === 0) {
        // Nothing to apply: an empty choice would only clear the pickers.
        const fresh = await run(() => fetchVacationExportInitial());
        const known = fresh?.favorites.some((f) => f.id === id) ?? true;
        if (fresh) setFavorites(fresh.favorites);
        if (!known && id === currentId) setCurrentId(null);
        toast.warning(
          known
            ? t("vacation.export.favorite.nothingPickable")
            : t("vacation.export.favorite.gone")
        );
        return;
      }
      apply(selection.users, selection.groups);
      setCurrentId(id);
    },
    create: async (name: string) => {
      const next = await run(() => createVacationExportFavorite(name, choice));
      if (!next) return;
      // The new one: the id the list didn't have before.
      const known = new Set(favorites.map((f) => f.id));
      setFavorites(next);
      setCurrentId(next.find((f) => !known.has(f.id))?.id ?? null);
    },
    update: async (id: number) => {
      const next = await run(() => updateVacationExportFavorite(id, choice));
      if (!next) return;
      setFavorites(next);
      setCurrentId(id);
    },
    rename: async (id: number, newName: string) => {
      const next = await run(() => renameVacationExportFavorite(id, newName));
      if (next) setFavorites(next);
    },
    remove: async (id: number) => {
      const next = await run(() => deleteVacationExportFavorite(id));
      if (!next) return;
      setFavorites(next);
      if (id === currentId) setCurrentId(null);
    },
  };
}
