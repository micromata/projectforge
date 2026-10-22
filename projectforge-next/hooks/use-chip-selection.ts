"use client";

import { useCallback, useRef, useState } from "react";

/** The modifier keys of the click that selects, see [useChipSelection]. */
export interface SelectModifiers {
  shiftKey: boolean;
  ctrlKey: boolean;
  metaKey: boolean;
}

/**
 * Which of a picker's chips are selected, picked as in a mail client's recipients: a click selects one
 * chip alone, Ctrl/Cmd+click adds or removes one, Shift+click selects the range from the last clicked.
 *
 * `order` is the ids as shown (sorted or not), so a range is what the user sees between the two chips.
 * Ids no longer in it count as unselected — a removed chip leaves nothing behind.
 */
export function useChipSelection(order: readonly number[]) {
  const [selected, setSelected] = useState<ReadonlySet<number>>(new Set());
  const anchor = useRef<number | null>(null);

  const select = useCallback(
    (id: number, { shiftKey, ctrlKey, metaKey }: SelectModifiers) => {
      const from = anchor.current == null ? -1 : order.indexOf(anchor.current);
      if (shiftKey && from >= 0) {
        const to = order.indexOf(id);
        const [lo, hi] = from < to ? [from, to] : [to, from];
        setSelected(new Set(order.slice(lo, hi + 1)));
        return;
      }
      anchor.current = id;
      if (ctrlKey || metaKey) {
        setSelected((current) => {
          const next = new Set(current);
          if (!next.delete(id)) next.add(id);
          return next;
        });
      } else {
        setSelected(new Set([id]));
      }
    },
    [order]
  );

  const clear = useCallback(() => {
    anchor.current = null;
    setSelected((current) => (current.size === 0 ? current : new Set()));
  }, []);

  const selectAll = useCallback(() => setSelected(new Set(order)), [order]);

  const live = order.filter((id) => selected.has(id));
  return {
    isSelected: (id: number) => selected.has(id) && order.includes(id),
    /** The selected ids, in the order shown. */
    selectedIds: live,
    select,
    clear,
    selectAll,
  };
}
