"use client";

import { useStore } from "@tanstack/react-form";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors, type FieldErrorMeta } from "./use-field-errors";

/** One end of the period: the form value it is held in, and how it is named on its own. */
export interface DatePeriodBound {
  /** Name of the form value, e.g. `periodOfPerformanceBegin`. */
  name: string;
  /** Accessible name of this box alone, e.g. "Leistungszeitraum von". */
  label: string;
}

/** One end as [DatePeriodField] renders it, plus whether it is mandatory. */
export interface DatePeriodGroupBound extends DatePeriodBound {
  required: boolean;
}

/**
 * What the two ends of a period need to be shown as one value: their rules, their current values, and
 * the single error line they share.
 *
 * Extracted from [DatePeriodField] so the component is layout and nothing else. The reads go through
 * the form's store rather than one `form.Field` subscription per box, because the group needs the meta
 * of *both* ends at once to decide whether to show that one error line at all.
 */
export function useDatePeriodGroup(
  begin: DatePeriodBound,
  end: DatePeriodBound
): {
  bounds: DatePeriodGroupBound[];
  /** Current value per bound, in the same order — `yyyy-MM-dd`, or empty. */
  values: (string | null | undefined)[];
  invalid: boolean;
  /** The sentences to print under the group, deduplicated. */
  errors: string[];
} {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const beginMeta = useFieldMetadata(begin.name);
  const endMeta = useFieldMetadata(end.name);
  const bounds = [
    { ...begin, required: beginMeta.required },
    { ...end, required: endMeta.required },
  ];

  const metas = useStore(form.store, (state) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    bounds.map((b) => (state as any).fieldMeta[b.name])
  ) as (FieldErrorMeta & { isTouched?: boolean; isValid?: boolean })[];

  // The other end's date, so an empty box opens its calendar in the month of the one that is filled
  // in rather than in the current one — the two ends of a period lie close together, and the end is
  // typically entered right after the begin (see DateInput's `defaultMonth`).
  const values = useStore(form.store, (state) =>
    bounds.map(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (b) => (state as any).values[b.name] as string | null | undefined
    )
  );

  const invalid =
    metas.some((m) => m?.isTouched) &&
    metas.some((m) => m && m.isValid === false);
  // Deduplicated: the same sentence from both ends would otherwise be printed twice.
  const errors = [
    ...new Set(
      metas.flatMap((m, i) => (m ? fieldErrors(m, bounds[i].label) : []))
    ),
  ];

  return { bounds, values, invalid, errors };
}
