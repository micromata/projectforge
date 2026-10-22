"use client";

import { useRef } from "react";

/** How many changes Undo goes back at most. */
const DEPTH = 50;

/**
 * Undo and redo for a control that is no text field, whose changes the browser does not track — the
 * chips of a picker. `change` stands in for `onChange` and remembers the value it replaces; `undo` and
 * `redo` walk back and forth through what was remembered.
 *
 * A value set from outside (a form reset, a loaded entry) is no step of the user's: once `value` is no
 * longer what was set here last (by `same`), the history is dropped rather than undone into.
 */
export function useUndoHistory<T>(
  value: T,
  onChange: (value: T) => void,
  same: (a: T, b: T) => boolean
) {
  const past = useRef<T[]>([]);
  const future = useRef<T[]>([]);
  const lastSet = useRef<T | null>(null);

  const current = () => {
    if (lastSet.current === null || !same(lastSet.current, value)) {
      past.current = [];
      future.current = [];
    }
    return value;
  };
  const set = (next: T) => {
    lastSet.current = next;
    onChange(next);
  };

  return {
    change: (next: T) => {
      past.current = [...past.current, current()].slice(-DEPTH);
      future.current = [];
      set(next);
    },
    /** Whether there was something to undo. */
    undo: () => {
      const now = current();
      const previous = past.current.pop();
      if (previous === undefined) return false;
      future.current.push(now);
      set(previous);
      return true;
    },
    /** Whether there was something to redo. */
    redo: () => {
      const now = current();
      const next = future.current.pop();
      if (next === undefined) return false;
      past.current.push(now);
      set(next);
      return true;
    },
  };
}
