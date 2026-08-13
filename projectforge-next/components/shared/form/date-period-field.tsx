"use client";

import { useStore } from "@tanstack/react-form";
import { FieldError } from "@/components/ui/field";
import { DateInput } from "@/components/shared/date-input";
import { cn } from "@/lib/utils";
import { FieldHint } from "./field-hint";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors, type FieldErrorMeta } from "./use-field-errors";

/** One end of the period: the form value it is held in, and how it is named on its own. */
export interface DatePeriodBound {
  /** Name of the form value, e.g. `periodOfPerformanceBegin`. */
  name: string;
  /** Accessible name of this box alone, e.g. "Leistungszeitraum von". */
  label: string;
}

export interface DatePeriodFieldProps {
  /** Name of the whole period, e.g. the translation of `fibu.periodOfPerformance._`. */
  label: string;
  begin: DatePeriodBound;
  end: DatePeriodBound;
  hint?: string;
  className?: string;
  /** Both boxes; a period is shown-but-not-fillable as a whole, never half of it. */
  disabled?: boolean;
}

/**
 * A period as the user thinks of it: one label, a start and an end, each of which may be empty.
 *
 * Two `form.Field`s rather than one composite value, for the same reason [SegmentedNumberField] keeps
 * one per box: the entity has a property per end (`periodOfPerformanceBegin`/`-End`), and a server side
 * complaint carrying `causedByField = "periodOfPerformanceEnd"` has to land on this group instead of
 * nowhere (see `applyServerValidationErrors`). They share the legend and one error line, because to the
 * user this is a single value — "Leistungszeitraum von" and "… bis" repeated underneath would be noise.
 *
 * Which end is mandatory is not decided here and cannot be: an order's begin becomes mandatory as soon
 * as a position inherits the period, a position's end as soon as it has its own one — that is
 * `PeriodOfPerformanceValidator`, and it stays there. The asterisk follows the metadata, the rest
 * arrives as an HTTP 406 on the field it names.
 */
export function DatePeriodField({
  label,
  begin,
  end,
  hint,
  className,
  disabled,
}: DatePeriodFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const beginMeta = useFieldMetadata(begin.name);
  const endMeta = useFieldMetadata(end.name);
  const bounds = [
    { ...begin, required: beginMeta.required },
    { ...end, required: endMeta.required },
  ];

  // The form's store rather than one `form.Field` subscription per box: the group needs the meta of
  // both ends at once to decide whether to show its single shared error line.
  const metas = useStore(form.store, (state) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    bounds.map((b) => (state as any).fieldMeta[b.name])
  ) as (FieldErrorMeta & { isTouched?: boolean; isValid?: boolean })[];

  // The other end's date, so an empty box opens its calendar in the month of the one that is filled
  // in rather than in the current one — the two ends of a period lie close together, and the end is
  // typically entered right after the begin (see DateInput's `defaultMonth`).
  const values = useStore(form.store, (state) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    bounds.map(
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

  return (
    <fieldset
      data-invalid={invalid || undefined}
      // A container, so the two ends arrange themselves by the width they actually got rather than by
      // the viewport: the same field sits in a third of an edit page and in a narrow position row.
      className={cn("@container flex min-w-0 flex-col gap-1.5", className)}
    >
      <legend className="inline-flex items-center gap-1 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
        {bounds.some((b) => b.required) && !disabled && (
          <span className="ml-0.5 text-primary">*</span>
        )}
        {hint && <FieldHint hint={hint} label={label} />}
      </legend>
      {/* Side by side where one column of the grid has room for both — otherwise stacked, because two
          date fields squeezed into that width truncate the very dates they show ("24.07.2"). The
          threshold is what two of them plus the dash actually need (@2xs, 18rem); a date field is
          bounded to the width of one date (see DateInput), so more room only leaves a gap. */}
      <div className="flex flex-col gap-1.5 @2xs:flex-row @2xs:items-center">
        {bounds.map((bound, index) => (
          <div
            key={bound.name}
            // Only as wide as one date needs, not half of whatever the column offers: a period reads
            // as one value, and stretching the two ends apart pulls the dash into empty space.
            className="flex min-w-0 shrink items-center gap-1.5"
          >
            {index > 0 && (
              // Decorative: each box is named by its own `aria-label` already. Only between them, so
              // stacked they simply sit under each other.
              <span
                aria-hidden
                className="hidden text-muted-foreground @2xs:inline"
              >
                –
              </span>
            )}
            <form.Field name={bound.name as never}>
              {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
              {(field: any) => (
                <DateInput
                  // Half of the group, shrinkable below its natural width: the two ends share the
                  // space of a single field, and neither grows past one date's worth of it.
                  className="min-w-0 flex-1"
                  aria-label={bound.label}
                  value={field.state.value as string | null}
                  defaultMonth={values[index === 0 ? 1 : 0]}
                  invalid={invalid}
                  required={bound.required}
                  disabled={disabled}
                  onChange={(next) => field.handleChange(next)}
                  onBlur={field.handleBlur}
                />
              )}
            </form.Field>
          </div>
        ))}
      </div>
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </fieldset>
  );
}
