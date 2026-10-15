"use client";

import { useCallback } from "react";
import {
  useKeyboardNavState,
  type KeyboardNav,
} from "@/components/data-table/use-keyboard-nav";
import type { TaskNode } from "@/lib/rs/task";

/**
 * File-explorer keyboard control for the structure tree: `↑`/`↓` move the focus between the visible
 * rows, `→` expands the focused element and `←` collapses it, and `Enter` opens it.
 *
 * Only expanding and collapsing, no jumping to the first child or the parent — the tree's expansion is
 * a server round-trip (see use-task-tree.ts), so every `→`/`←` is a request and moving the focus onto a
 * child that isn't loaded yet has nothing to land on. The vertical movement and the focus state are the
 * generic [useKeyboardNavState]; this only adds what a key means for a tree node.
 *
 * `toggle` and `select` are the panel's own handlers (see TaskTreePanel): `toggle` opens a closed node
 * and closes an open one, so `→` calls it only on a `CLOSED` node and `←` only on an `OPENED` one —
 * the direction is decided here, the effect is the same one a click on the chevron has.
 */
export function useTreeKeyboard(
  nodes: TaskNode[],
  toggle: (task: TaskNode) => void,
  select: (task: TaskNode) => void,
  /** The task the tree opens on: the keyboard cursor starts on it, marked and scrolled into view. */
  initialFocusId?: number | null
): KeyboardNav {
  const rowIds = useCallback(
    () => nodes.map((node) => String(node.id)),
    [nodes]
  );
  const { focusedRowId, focusRow, move, focusedIndex } = useKeyboardNavState(
    rowIds,
    initialFocusId != null ? String(initialFocusId) : null
  );

  const onKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        move(event.key === "ArrowDown" ? 1 : -1);
        return;
      }
      const node = nodes[focusedIndex()];
      if (!node) return;
      if (event.key === "ArrowRight") {
        if (node.treeStatus === "CLOSED") {
          event.preventDefault();
          toggle(node);
        }
      } else if (event.key === "ArrowLeft") {
        if (node.treeStatus === "OPENED") {
          event.preventDefault();
          toggle(node);
        }
      } else if (event.key === "Enter") {
        event.preventDefault();
        select(node);
      }
    },
    [nodes, focusedIndex, move, toggle, select]
  );

  return { focusedRowId, focusRow, onKeyDown };
}
