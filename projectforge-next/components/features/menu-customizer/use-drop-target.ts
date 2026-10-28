"use client";

import { useDndContext, type Active, type Over } from "@dnd-kit/core";
import { itemId } from "./menu-tree";
import { FAVORITES, TEMPLATE, type DragData } from "./types";

/** Where the dragged entry would land: in [container], next to the entry [id] or at its end. */
export interface DropTarget {
  container: string;
  id?: string;
  /** Behind [id] rather than before it. */
  after: boolean;
}

/**
 * Whether the dragged entry is past the middle of the entry it is over — below it in a group, right of it
 * in the bar — and so goes behind it: the only way to the last place of a list.
 */
export function dropsAfter(active: Active, over: Over): boolean {
  const dragged = active.rect.current.translated;
  if (!dragged) return false;
  const horizontal =
    (over.data.current as DragData | undefined)?.container === FAVORITES;
  return horizontal
    ? dragged.left + dragged.width / 2 > over.rect.left + over.rect.width / 2
    : dragged.top + dragged.height / 2 > over.rect.top + over.rect.height / 2;
}

/**
 * The drop target of the current drag, if it moves the entry into another list (from the template, into
 * a group, out of one) — the case a sortable list doesn't show by making room. The rules of applyDrop.
 */
export function useDropTarget(): DropTarget | null {
  const { active, over } = useDndContext();
  const dragged = active?.data.current as DragData | undefined;
  const target = over?.data.current as DragData | undefined;
  if (!active || !over || !dragged?.item || !target) return null;
  if (target.container === TEMPLATE) return null;
  if (dragged.kind === "group" && target.container !== FAVORITES) return null;
  if (dragged.container === target.container) return null;
  return {
    container: target.container,
    id: target.item ? itemId(target.item) : undefined,
    after: !!target.item && dropsAfter(active, over),
  };
}
