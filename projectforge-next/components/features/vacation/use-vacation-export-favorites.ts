"use client";

import { useState } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  createVacationExportFavorite,
  deleteVacationExportFavorite,
  renameVacationExportFavorite,
  selectVacationExportFavorite,
  updateVacationExportFavorite,
} from "@/lib/rs/vacation-export";
import { toast } from "@/lib/toast";
import type { VacationExportFavorite, VacationExportRef } from "./types";

/** Whether both hold the same ids, in whatever order. */
function sameIds(a: number[], b: number[]): boolean {
  const set = new Set(a);
  return set.size === new Set(b).size && b.every((id) => set.has(id));
}

/**
 * The favorites of the team vacation calendar: named choices of employees and groups (stored by the backend with
 * the user's choice, `VacationExportRest`), bound to the presentational [FavoritesMenu].
 *
 * The favorite applied last is the current one; it reads as modified as soon as the employees or groups on screen
 * differ from it. Applying one takes only what the user may (still) pick.
 */
export function useVacationExportFavorites({
  initial,
  employees,
  groups,
  apply,
}: {
  initial: VacationExportFavorite[];
  employees: EntityRef[];
  groups: EntityRef[];
  apply: (employees: VacationExportRef[], groups: VacationExportRef[]) => void;
}) {
  const [favorites, setFavorites] = useState(initial);
  const choice = {
    employeeIds: employees.map((e) => e.id),
    groupIds: groups.map((g) => g.id),
  };
  // Without one applied yet, the favorite holding exactly the choice on screen (e.g. the stored one).
  const [currentId, setCurrentId] = useState<number | null>(
    () =>
      initial.find(
        (f) =>
          sameIds(f.employeeIds, choice.employeeIds) &&
          sameIds(f.groupIds, choice.groupIds)
      )?.id ?? null
  );
  const current = favorites.find((f) => f.id === currentId) ?? null;
  const isModified =
    current != null &&
    !(
      sameIds(current.employeeIds, choice.employeeIds) &&
      sameIds(current.groupIds, choice.groupIds)
    );

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
      apply(selection.employees, selection.groups);
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
