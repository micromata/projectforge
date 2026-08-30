"use client";

import { useCallback, useState } from "react";

/**
 * What a table needs to be driven from the keyboard, the counterpart of [RowSelection] for the plain
 * "one focused row" case: the row the keys are on, a way to move that focus in from a click, and the
 * key handler itself. Produced by a hook that knows the rows and what a key means for them — the
 * structure tree's [useTreeKeyboard] is the one caller so far.
 *
 * Generic on purpose: DataTable makes the body focusable, marks the focused row and scrolls it into
 * view for either this or a selection, so the domain's key semantics (a tree expands, a flat list may
 * only move) stay out of the table.
 */
export interface KeyboardNav {
  /** The row the keyboard is on, by row id, so the table can mark it (`row-focused`). */
  focusedRowId: string | null;
  /** Moves the focus onto a row the user clicked, so the arrow keys continue from there. */
  focusRow: (rowId: string) => void;
  onKeyDown: (event: React.KeyboardEvent) => void;
}

/**
 * The shared focus state and the ↑/↓ movement every keyboard-navigable table has, split from the
 * key semantics that differ per table. A caller wraps this and adds its own keys (the tree adds
 * →/←/Enter, see useTreeKeyboard), delegating the vertical movement here.
 *
 * The row ids are taken as a function evaluated on each key, not as an array captured once: the rows
 * change under the focus (a filter, a refetch, a tree expanding), and the movement has to be over the
 * set on screen at the moment the key is pressed.
 *
 * `initialFocusId` seeds the focus at mount so the keyboard cursor can start on a meaningful row rather
 * than nowhere — the structure tree opens with it on the task the field already holds (see
 * useTreeKeyboard). It is read once, on mount; moving the focus afterwards is the user's to do.
 */
export function useKeyboardNavState(
  rowIds: () => string[],
  initialFocusId?: string | null
): {
  focusedRowId: string | null;
  focusRow: (rowId: string) => void;
  /** Moves the focus by `step` rows, clamped; from nothing, ↓ starts at the top and ↑ at the bottom. */
  move: (step: number) => void;
  /** The row the focus is on right now, or undefined — for a caller deciding what a key does to it. */
  focusedIndex: () => number;
} {
  const [focusedRowId, setFocusedRowId] = useState<string | null>(
    initialFocusId ?? null
  );

  const focusedIndex = useCallback(() => {
    if (!focusedRowId) return -1;
    return rowIds().indexOf(focusedRowId);
  }, [rowIds, focusedRowId]);

  const move = useCallback(
    (step: number) => {
      const ids = rowIds();
      if (ids.length === 0) return;
      const current = focusedRowId ? ids.indexOf(focusedRowId) : -1;
      const next =
        current < 0
          ? step > 0
            ? 0
            : ids.length - 1
          : Math.min(Math.max(current + step, 0), ids.length - 1);
      setFocusedRowId(ids[next]);
    },
    [rowIds, focusedRowId]
  );

  const focusRow = useCallback((rowId: string) => setFocusedRowId(rowId), []);

  return { focusedRowId, focusRow, move, focusedIndex };
}
