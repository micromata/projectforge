"use client";

import { useState } from "react";
import { useFormatContext } from "@/hooks/use-format";
import {
  periodKindOf,
  periodKindsOf,
  type PeriodKind,
  type PeriodKindId,
} from "@/lib/date-period";
import {
  endOfPeriod,
  periodOfBounds,
  shiftBounds,
} from "@/lib/date-period-bounds";
import { useEntityEditForm } from "./form-context";

/**
 * The art a period is given as on a form, and what picking one or moving the begin does to the end.
 *
 * The art is **derived from the two values** (`periodOfBounds`) rather than stored: the entity has no such
 * property, so a loaded order shows the term it happens to be, and an end typed by hand dissolves it
 * without anything having to be told.
 *
 * One bit of memory is still needed, and only for one path: emptying the begin (the field's clear button,
 * or backspacing it away) leaves nothing to measure, and a purely derived selection would fall away there
 * — the next date typed would then no longer drag the end along, which is the whole point. So the
 * selection is kept, and re-read from the values on **every** render in which both ends are there.
 * Adjusted during render rather than in an effect, as DateInput does with its text: a form reset after the
 * entity loaded, the values coming back from a save, and a position row whose subtree React re-used for
 * another index (see RepeatableList, which keys by index) all correct themselves that way, with no second
 * render pass and nothing to keep in sync.
 *
 * The recomputing hangs off the begin box's `onChange`, never off an effect. The handler knows *who*
 * changed the value — an effect sees only that it differs and could not tell a user's edit from
 * `form.reset(...)`, so it would fight the reset — and it still sees the consistent pair, hence the art
 * that was in effect a moment ago. `payment-terms-fields.tsx` writes across fields from a handler for the
 * same reason.
 */
export function useDatePeriodKind({
  beginName,
  endName,
  begin,
  end,
  ids,
}: {
  beginName: string;
  endName: string;
  /** The current values, as [useDatePeriodGroup] read them off the store. */
  begin: string | null | undefined;
  end: string | null | undefined;
  ids: readonly PeriodKindId[] | undefined;
}): {
  /** The arts offered; empty means no picker at all. */
  kinds: PeriodKind[];
  /** The art in effect, or null. */
  kind: PeriodKind | null;
  /** From the begin box: moves the end along with the art in effect. */
  onBeginChanged: (next: string | null) => void;
  /** From the picker: writes the end, and the begin where there was none. */
  onKindSelected: (kind: PeriodKind, anchor: string) => void;
  /** Whether the period has a length to be paged by — false leaves the arrows disabled. */
  canStep: boolean;
  /** From the arrows: the whole period `steps` of its own lengths on. */
  onStep: (steps: number) => void;
} {
  const form = useEntityEditForm();
  const ctx = useFormatContext();
  const kinds = periodKindsOf(ids);
  const [selected, setSelected] = useState<PeriodKindId | null>(null);
  const measured = periodOfBounds(begin, end, kinds, ctx)?.kind.id ?? null;

  const [synced, setSynced] = useState({ begin, end });
  if (synced.begin !== begin || synced.end !== end) {
    setSynced({ begin, end });
    // Only while both ends are there: with one of them empty there is nothing to measure, and dropping
    // the selection would re-open the very hole it exists for.
    if (begin && end && measured !== selected) setSelected(measured);
  }
  const kind = periodKindOf(selected);

  return {
    kinds,
    kind,
    onBeginChanged: (next) => {
      // An emptied begin leaves the end alone — deleting a value is how one is retyped, and throwing the
      // other end away over a keystroke is not what that means.
      if (!next || !kind) return;
      const computed = endOfPeriod(next, kind, ctx);
      if (computed) form.setFieldValue(endName, computed);
    },
    onKindSelected: (picked, anchor) => {
      // The anchor is the stepper's: the begin that stands there, or today where the box is empty — a term
      // with no visible begin would be invisible state, and "ab heute" is the obvious intent.
      if (!begin) form.setFieldValue(beginName, anchor);
      const computed = endOfPeriod(anchor, picked, ctx);
      if (computed) form.setFieldValue(endName, computed);
      // Explicitly, for the render in which the values do not show it yet.
      setSelected(picked.id);
    },
    canStep: shiftBounds(begin, end, kind, 1, ctx) !== null,
    onStep: (steps) => {
      const moved = shiftBounds(begin, end, kind, steps, ctx);
      if (!moved) return;
      form.setFieldValue(beginName, moved.from);
      form.setFieldValue(endName, moved.to);
      // Nothing to do about the selection: the moved period is the same art measured off its new begin, so
      // the render sync above reads back exactly what is in effect now — including "none", where the
      // period was entered by hand and paged by its day count.
    },
  };
}
