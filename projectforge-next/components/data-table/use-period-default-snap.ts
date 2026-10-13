"use client";

import { useEffect, useRef, useState } from "react";
import type { PeriodKind } from "@/lib/date-period";
import type { FormatContext } from "@/lib/format";
import type { MagicFilterEntryValue } from "@/lib/rs/types";
import { editedDateValue } from "./filter-period";

type Part = "from" | "to";

/**
 * The typing of a DATE filter's two bounds where the list has a default art ([PageDef.filterDefaultPeriodKind],
 * the calendar year of the vacations): a date typed snaps to the period of that art around it — but only once
 * the box is left or Enter pressed, not on the way there. A strict parse already takes "14.05.20" for a date
 * (two-digit year), and snapping that to 01.01.–31.12.2020 would put "31.12.2020" into the box under the
 * user's fingers, the "27" still to come appended to it.
 *
 * While typing, the bound goes out as typed (live, as every pill edit does) and free; [commit] then applies
 * [editedDateValue] with the default art to the value the typing began on. A box whose date was snapped is
 * rebuilt from the value ([revision] as its key): it keeps its own text otherwise, and an end snapped to the
 * 31.12. it already was is no change it would notice.
 *
 * Without a default art every edit is [editedDateValue] at once, as before.
 */
export function usePeriodDefaultSnap(
  value: MagicFilterEntryValue | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext,
  defaultKind: PeriodKind | null,
  onChange: (value: MagicFilterEntryValue | undefined) => void
) {
  const [revision, setRevision] = useState(0);
  // The bound being typed, the value its typing began on and the date last reported for it.
  const typing = useRef<{
    part: Part;
    base: MagicFilterEntryValue | undefined;
    raw: string | null;
  } | null>(null);

  function change(part: Part, raw: string | null) {
    if (!defaultKind) {
      onChange(editedDateValue(value, part, raw, kinds, ctx));
      return;
    }
    const base = typing.current?.part === part ? typing.current.base : value;
    typing.current = { part, base, raw };
    const typed = { ...base, periodKind: undefined, [part]: raw ?? undefined };
    onChange(typed.from || typed.to ? typed : undefined);
  }

  /** The value once the bound typed is done with, snapped; undefined if nothing was typed into [part]. */
  function commit(part: Part, raw?: string | null) {
    const pending = typing.current;
    if (!pending || pending.part !== part) return undefined;
    typing.current = null;
    const date = raw === undefined ? pending.raw : raw;
    const result = editedDateValue(
      pending.base,
      part,
      date,
      kinds,
      ctx,
      defaultKind
    );
    if (date && result?.[part] !== date) setRevision((r) => r + 1);
    return result;
  }

  /** On leaving the box: the snapped value goes out. */
  function blur(part: Part) {
    if (typing.current?.part !== part) return;
    onChange(commit(part));
  }

  /** On Enter, with the date [DateInput] just committed: the value to save the pill with. */
  function submit(part: Part, raw: string | null) {
    if (!defaultKind) return editedDateValue(value, part, raw, kinds, ctx);
    const result =
      commit(part, raw) ??
      editedDateValue(value, part, raw, kinds, ctx, defaultKind);
    onChange(result);
    return result;
  }

  // A pill closed without the box being left first (Escape): what was typed is snapped all the same.
  const pendingBlur = useRef(blur);
  useEffect(() => {
    pendingBlur.current = blur;
  });
  useEffect(
    () => () => {
      const part = typing.current?.part;
      if (part) pendingBlur.current(part);
    },
    []
  );

  return { revision, change, blur, submit };
}
