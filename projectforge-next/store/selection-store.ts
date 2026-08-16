import { create } from "zustand";
import type { RowSelectionState } from "@tanstack/react-table";

/** The selection mode of one entity's list: whether it is on, and what is ticked. */
interface EntitySelection {
  /**
   * Whether the list is in selection mode — the checkboxes, the keyboard and "a click selects" are
   * only there while it is (see useListSelection).
   */
  active: boolean;
  /** TanStack's row selection, keyed by row id, which is the entity's id (`getRowId`). */
  rows: RowSelectionState;
}

interface SelectionState {
  byEntity: Record<string, EntitySelection>;
  /** Switches the mode on, keeping whatever was ticked before. */
  enter: (entity: string) => void;
  /**
   * Switches it off and drops the ticks — but keeps the entry, so the restore below cannot bring the
   * mode straight back from a `listMeta` still cached with the old ids.
   */
  leave: (entity: string) => void;
  setRows: (
    entity: string,
    rows:
      | RowSelectionState
      | ((previous: RowSelectionState) => RowSelectionState)
  ) => void;
  /**
   * Takes the selection the backend remembered for this entity (`listMeta.selectedIds`) — and only
   * while this app knows nothing about it yet.
   *
   * That condition is what makes it a *restore* rather than a second source of truth: after a reload
   * the store is empty and the session's ids are the truth, while during in-app navigation the store
   * is ahead of the cached `listMeta` and must win. Restoring switches the mode on: ticks nobody can
   * see would be a selection the user cannot tell is there.
   */
  restore: (entity: string, ids: number[]) => void;
}

const EMPTY: EntitySelection = { active: false, rows: {} };

export const useSelectionStore = create<SelectionState>((set) => ({
  byEntity: {},
  enter: (entity) =>
    set((state) => ({
      byEntity: {
        ...state.byEntity,
        [entity]: { ...(state.byEntity[entity] ?? EMPTY), active: true },
      },
    })),
  leave: (entity) =>
    set((state) => ({
      byEntity: { ...state.byEntity, [entity]: { active: false, rows: {} } },
    })),
  setRows: (entity, rows) =>
    set((state) => {
      const current = state.byEntity[entity] ?? EMPTY;
      const next = typeof rows === "function" ? rows(current.rows) : rows;
      return {
        byEntity: { ...state.byEntity, [entity]: { ...current, rows: next } },
      };
    }),
  restore: (entity, ids) =>
    set((state) => {
      if (state.byEntity[entity] || ids.length === 0) return state;
      const rows: RowSelectionState = {};
      for (const id of ids) rows[String(id)] = true;
      return {
        byEntity: { ...state.byEntity, [entity]: { active: true, rows } },
      };
    }),
}));

/** The mode and the ticks of one entity, or the empty pair for a list nothing is known about yet. */
export function useEntitySelection(entity: string): EntitySelection {
  return useSelectionStore((state) => state.byEntity[entity] ?? EMPTY);
}
