"use client";

import { useCallback, useMemo, useRef, useState } from "react";
import type { BusinessCardField, IncludedFields } from "./types";

/**
 * Which fields are on the card. Kept beside the form (they are no values of the card) and readable by the
 * save through [current]: the form keeps the save callback of its first render. [changed] tells whether the
 * ticks differ from the ones the editor started with, which is a change to save like a changed value.
 */
export function useIncludedFields(initial: () => IncludedFields) {
  const [start] = useState<IncludedFields>(initial);
  const [included, setState] = useState<IncludedFields>(start);
  const ref = useRef(included);
  const set = useCallback((next: IncludedFields) => {
    ref.current = next;
    setState(next);
  }, []);
  const toggle = useCallback(
    (field: BusinessCardField, value: boolean) =>
      set({ ...ref.current, [field]: value }),
    [set]
  );
  const current = useCallback(() => ref.current, []);
  const changed = useMemo(
    () =>
      (Object.keys(start) as BusinessCardField[]).some(
        (field) => start[field] !== included[field]
      ),
    [start, included]
  );
  return { included, set, toggle, current, changed };
}
