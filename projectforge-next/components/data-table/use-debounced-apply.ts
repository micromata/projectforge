"use client";

import { useEffect, useRef } from "react";
import { useDebouncedValue } from "@/hooks/use-debounced-value";

/**
 * Long enough to coalesce a run of stepper clicks and the from/to pair into one query, short enough
 * not to feel like a delay. The same figure as the search box (SEARCH_DELAY_MS): both settle into a
 * full `MagicFilter` server list query.
 */
export const FILTER_APPLY_DELAY_MS = 300;

/**
 * Applies a pill's `draft` to the list automatically once editing settles, without a save click.
 *
 * The same shape as [SearchInput]: the popover renders `draft` instantly, but only the debounced
 * value leaves — and only when it differs from what was last applied. `applied` is the committed
 * value coming back down; tracking it lets the hook swallow the redundant apply on open (draft and
 * applied still match) and the value round-trip (our own apply returns as `applied`).
 *
 * Compared by content, not by reference, the way `useMagicFilterQuery` serializes its entries: the
 * draft and the committed value are distinct objects even when equal (the history pill rebuilds its
 * picked values every render), so a reference check would misfire on open.
 */
export function useDebouncedApply<T>(
  draft: T,
  applied: T,
  onApply: (value: T) => void,
  delay = FILTER_APPLY_DELAY_MS
): void {
  const debounced = useDebouncedValue(draft, delay);
  const appliedKey = JSON.stringify(applied ?? null);
  const debouncedKey = JSON.stringify(debounced ?? null);
  const sent = useRef(appliedKey);

  // The committed value wins whenever it changes for a reason other than this pill's own edit — a
  // reset, or a saved filter applied — so the pending edit is not fired again against a value that
  // moved underneath it.
  useEffect(() => {
    sent.current = appliedKey;
  }, [appliedKey]);

  useEffect(() => {
    if (debouncedKey === sent.current) return;
    sent.current = debouncedKey;
    onApply(debounced);
    // onApply is a fresh closure per render on most call sites; depending on it would fire this on
    // every render rather than on a settled value.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKey]);
}
