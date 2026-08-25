"use client";

import { FieldError } from "@/components/ui/field";
import { PeriodStepper } from "@/components/shared/period-stepper";
import type { PeriodKindId } from "@/lib/date-period";
import { cn } from "@/lib/utils";
import { DatePeriodBoundsRow } from "./date-period-bounds-row";
import { FieldHint } from "./field-hint";
import { useDatePeriodKind } from "./use-date-period-kind";
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
   * Arts offered beside the two boxes ("1 Monat"), so only the begin has to be entered — the end follows
   * it. Absent or empty means two plain dates: for most periods the two ends are unrelated. A form offers
   * the terms (`TERM_KIND_IDS`), never `yearToDate`: an agreed period of performance ends on a date, not
   * "today".
   */
  periodKinds?: readonly PeriodKindId[];
  /**
   * Whether to offer the paging arrows, which move the whole period on by its own length. Independent of
   * [periodKinds]: a period entered by hand pages by the days it spans.
   */
  paging?: boolean;
  /**
   * Spell the art out in its trigger ("3 Monate") rather than abbreviate it ("3M"), for a grid cell with
   * the room for it (see [PeriodQuickSelect]). Off by default, as the narrow position grid needs.
   */
  longLabel?: boolean;
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
 * Where `periodKinds` are offered the period can also be given as a term: pick "3 Monate" and the end is
 * filled in from the begin, and from then on moving the begin moves the end with it. Which term is in
 * effect is read off the two dates rather than stored (there is no such property on the entity), so
 * editing the end by hand simply dissolves it — as does the picker's "Eigener Zeitraum", which lets the
 * term go while keeping the two dates so the begin can be moved on its own (see [useDatePeriodKind]). Where
 * `paging` is on, the arrows beside them move the whole period on by that term, or by its day count where
 * it is none.
 */
export function DatePeriodField({
  label,
  begin,
  end,
  hint,
  periodKinds,
  paging = false,
  longLabel = false,
  className,
  disabled,
}: DatePeriodFieldProps) {
  const { bounds, values, invalid, errors } = useDatePeriodGroup(begin, end);
  const {
    kinds,
    kind,
    onBeginChanged,
    onKindSelected,
    onCleared,
    canStep,
    onStep,
  } = useDatePeriodKind({
    beginName: begin.name,
    endName: end.name,
    begin: values[0],
    end: values[1],
    ids: periodKinds,
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
        <PeriodStepper
          kinds={kinds}
          // The begin as it stands is the anchor here — a term is measured off the date in the box, never
          // off a snapped one. No name between the arrows: the two dates say what the term is.
          current={kind && values[0] ? { kind, anchor: values[0] } : null}
          // And it is the anchor for the first pick as well, while no art is in effect yet: "3 Monate"
          // beside a begin of 15.03. means that term, not one beginning today.
          anchor={values[0]}
          onSelect={onKindSelected}
          // Lets the art go while keeping the two dates, so the begin can then be moved without the end
          // following — the picker offers it as a first "Eigener Zeitraum" entry only while an art is on.
          onClear={onCleared}
          paging={paging}
          canStep={canStep}
          onStep={onStep}
          longLabel={longLabel}
          disabled={disabled}
        />
      </div>
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </fieldset>
  );
}
