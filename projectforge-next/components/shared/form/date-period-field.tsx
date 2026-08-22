"use client";

import { FieldError } from "@/components/ui/field";
import type { DurationId } from "@/lib/date-duration";
import { cn } from "@/lib/utils";
import { DatePeriodBoundsRow } from "./date-period-bounds-row";
import { DatePeriodDurationStepper } from "./date-period-duration-stepper";
import { FieldHint } from "./field-hint";
import { useDatePeriodDuration } from "./use-date-period-duration";
import {
  useDatePeriodGroup,
  type DatePeriodBound,
} from "./use-date-period-group";

export type { DatePeriodBound };

export interface DatePeriodFieldProps {
  /** Name of the whole period, e.g. the translation of `fibu.periodOfPerformance._`. */
  label: string;
  begin: DatePeriodBound;
  end: DatePeriodBound;
  hint?: string;
  /**
   * Lengths offered beside the two boxes ("1 Monat"), so only the begin has to be entered — the end
   * follows it. Absent or empty means two plain dates: for most periods the two ends are unrelated.
   */
  durations?: readonly DurationId[];
  /**
   * Whether to offer the paging arrows, which move the whole period on by its own length. Independent of
   * [durations]: a period entered by hand pages by the days it spans.
   */
  paging?: boolean;
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
 *
 * Where `durations` are offered the period can also be given as a term: pick "3 Monate" and the end is
 * filled in from the begin, and from then on moving the begin moves the end with it. Which term is in
 * effect is read off the two dates rather than stored (there is no such property on the entity), so
 * editing the end by hand simply dissolves it — see [useDatePeriodDuration]. Where `paging` is on, the
 * arrows beside them move the whole period on by that term, or by its day count where it is none.
 */
export function DatePeriodField({
  label,
  begin,
  end,
  hint,
  durations: durationIds,
  paging = false,
  className,
  disabled,
}: DatePeriodFieldProps) {
  const { bounds, values, invalid, errors } = useDatePeriodGroup(begin, end);
  const {
    durations,
    duration,
    onBeginChanged,
    onDurationSelected,
    canStep,
    onStep,
  } = useDatePeriodDuration({
    beginName: begin.name,
    endName: end.name,
    begin: values[0],
    end: values[1],
    ids: durationIds,
  });

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
      {/* Wrapping rather than a breakpoint: the quick access is a few characters wide and belongs beside
          the two dates wherever it fits, and one line below wherever it does not. Which of the two that
          is depends on the date format and the label widths of the account, so it is left to the browser
          rather than guessed at a container width. */}
      <div className="flex min-w-0 flex-wrap items-center gap-x-1 gap-y-1.5">
        <DatePeriodBoundsRow
          bounds={bounds}
          values={values}
          invalid={invalid}
          disabled={disabled}
          onBeginChanged={onBeginChanged}
        />
        <DatePeriodDurationStepper
          durations={durations}
          value={duration}
          onSelect={onDurationSelected}
          paging={paging}
          canStep={canStep}
          onStep={onStep}
          disabled={disabled}
        />
      </div>
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </fieldset>
  );
}
