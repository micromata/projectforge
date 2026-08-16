"use client";

import { useCallback, useRef, useState } from "react";
import type { RowSelectionState } from "@tanstack/react-table";

/** What a click on a row means, as the modifier keys say it. */
type ClickModifiers = Pick<MouseEvent, "shiftKey" | "ctrlKey" | "metaKey">;

export interface RowSelection {
  /** The state TanStack holds, keyed by row id (see `getRowId`). */
  state: RowSelectionState;
  setState: React.Dispatch<React.SetStateAction<RowSelectionState>>;
  /** The ids the user picked, as numbers — what the backend is told (see lib/rs/multi-select.ts). */
  selectedIds: number[];
  clear: () => void;
  /**
   * Click on a row: Ctrl/Cmd toggles it, Shift extends from the anchor, and a plain click selects only
   * it. Always takes the click — inside the selection mode that is what a click *is*, and outside it
   * this hook's handlers are not wired up at all (see EntityListPage).
   *
   * Still answers a boolean, which is what tells [DataTableRow] not to open the entry as well.
   */
  onRowClick: (rowId: string, modifiers: ClickModifiers) => boolean;
  /** Arrow keys move the focus, Shift+Arrow extends, Space toggles the focused row. */
  onKeyDown: (event: React.KeyboardEvent) => void;
  /** Row the keyboard is on, so the table can mark it. */
  focusedRowId: string | null;
  /**
   * Puts the keyboard on the first row without selecting it — the anchor entering the mode sets, so
   * `↑`/`↓` and `Shift+↓` work at once instead of after a first click (see DataTable's focus effect).
   */
  focusFirstRow: () => void;
}

/**
 * Multi-selection over a table's rows, with the interaction the legacy grid established: click,
 * Ctrl/Cmd+click, Shift+click for a range, arrow keys, Shift+arrow, Space.
 *
 * Keyed by row *id*, never by index. Both list paths set `getRowId` to the entity's id (see
 * useEntityListPage), so a selection survives sorting, paging and a refetch — with indices, sorting the
 * table would move the selection to whichever row slid into that position.
 *
 * Takes the ids of the displayed rows as a *function* rather than the table, and for two reasons: the
 * table is created with this selection's state (so it does not exist yet when this runs), and the
 * ranges have to be taken over the order the user currently sees and drags over — which is read when
 * the click happens, not when the hook does.
 *
 * The ticked rows may be held outside (`state`/`setState`), because the selection outlives the list:
 * it is remembered per entity while the user visits the mass update page and comes back, and it is
 * posted to the backend from there (see useListSelection). Without them it keeps its own state, which
 * is what a table with no such lifetime needs.
 */
export function useRowSelection(
  displayedRowIds: () => string[],
  external?: {
    state: RowSelectionState;
    setState: React.Dispatch<React.SetStateAction<RowSelectionState>>;
  }
): RowSelection {
  const [internalState, setInternalState] = useState<RowSelectionState>({});
  const [focusedRowId, setFocusedRowId] = useState<string | null>(null);
  /** Where a Shift range starts: the row last clicked or toggled, as an id. */
  const anchorRowId = useRef<string | null>(null);

  const state = external?.state ?? internalState;
  const setExternalState = external?.setState;
  const setState = useCallback<
    React.Dispatch<React.SetStateAction<RowSelectionState>>
  >(
    (value) => (setExternalState ?? setInternalState)(value),
    [setExternalState]
  );

  const selectedIds = Object.keys(state)
    .filter((id) => state[id])
    .map(Number)
    .filter((id) => !Number.isNaN(id));

  const clear = useCallback(() => {
    setState({});
    setFocusedRowId(null);
    anchorRowId.current = null;
  }, [setState]);

  const focusFirstRow = useCallback(() => {
    const [first] = displayedRowIds();
    if (!first) return;
    setFocusedRowId(first);
    anchorRowId.current = first;
  }, [displayedRowIds]);

  const onRowClick = useCallback(
    (rowId: string, modifiers: ClickModifiers) => {
      const rowIds = displayedRowIds();
      setFocusedRowId(rowId);
      if (modifiers.shiftKey && anchorRowId.current) {
        const from = rowIds.indexOf(anchorRowId.current);
        const to = rowIds.indexOf(rowId);
        if (from < 0 || to < 0) return true;
        const range: RowSelectionState = {};
        for (let i = Math.min(from, to); i <= Math.max(from, to); i++) {
          range[rowIds[i]] = true;
        }
        // Replaces the selection rather than adding to it, as a range select does everywhere: the
        // anchor stays, so the user can widen and narrow the same range by clicking around.
        setState(range);
        return true;
      }
      anchorRowId.current = rowId;
      if (modifiers.ctrlKey || modifiers.metaKey) {
        setState((previous) => toggled(previous, rowId));
        return true;
      }
      setState({ [rowId]: true });
      return true;
    },
    [displayedRowIds, setState]
  );

  const onKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      const rowIds = displayedRowIds();
      if (rowIds.length === 0) return;
      const current = focusedRowId ? rowIds.indexOf(focusedRowId) : -1;

      if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        const step = event.key === "ArrowDown" ? 1 : -1;
        // From nowhere, down starts at the first row and up at the last one.
        const next =
          current < 0
            ? step > 0
              ? 0
              : rowIds.length - 1
            : Math.min(Math.max(current + step, 0), rowIds.length - 1);
        const nextId = rowIds[next];
        setFocusedRowId(nextId);
        if (event.shiftKey) {
          setState((previous) => ({ ...previous, [nextId]: true }));
        } else {
          anchorRowId.current = nextId;
        }
        return;
      }

      if (event.key === " " && focusedRowId) {
        event.preventDefault();
        setState((previous) => toggled(previous, focusedRowId));
        anchorRowId.current = focusedRowId;
      }
    },
    [displayedRowIds, focusedRowId, setState]
  );

  return {
    state,
    setState,
    selectedIds,
    clear,
    onRowClick,
    onKeyDown,
    focusedRowId,
    focusFirstRow,
  };
}

/** Adds the row to the selection, or takes it out if it was in it. */
function toggled(state: RowSelectionState, rowId: string): RowSelectionState {
  const next = { ...state };
  if (next[rowId]) {
    delete next[rowId];
  } else {
    next[rowId] = true;
  }
  return next;
}
