"use client";

import { DateInput } from "@/components/shared/date-input";
import { useEntityEditForm } from "./form-context";
import type { DatePeriodGroupBound } from "./use-date-period-group";

/**
 * The two date boxes of a period, `von – bis`.
 *
 * Split out of [DatePeriodField] so that component is the group as a whole — legend, quick access, error
 * line — and this one is the pair of boxes and how they arrange themselves. Both ends come from
 * [useDatePeriodGroup] already; nothing here decides anything about them.
 */
export function DatePeriodBoundsRow({
  bounds,
  values,
  invalid,
  disabled,
  onBeginChanged,
}: {
  bounds: DatePeriodGroupBound[];
  /** Current value per bound, in the same order. */
  values: (string | null | undefined)[];
  invalid: boolean;
  disabled?: boolean;
  /** Moves the end along where a duration is in effect — see [useDatePeriodDuration]. */
  onBeginChanged: (next: string | null) => void;
}) {
  const form = useEntityEditForm();

  return (
    // Side by side where one column of the grid has room for both — otherwise stacked, because two date
    // fields squeezed into that width truncate the very dates they show ("24.07.2"). The threshold is a
    // little above what two of them plus the dash need (@2xs, 18rem, against some 15rem); a date field is
    // bounded to the width of one date (see DateInput), so more room only leaves a gap.
    <div className="flex flex-col gap-1.5 @2xs:flex-row @2xs:items-center @2xs:gap-1">
      {bounds.map((bound, index) => (
        <div
          key={bound.name}
          // Only as wide as one date needs, not half of whatever the column offers: a period reads as one
          // value, and stretching the two ends apart pulls the dash into empty space.
          className="flex min-w-0 shrink items-center gap-1"
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
                // Half of the group, shrinkable below its natural width: the two ends share the space of
                // a single field, and neither grows past one date's worth of it.
                className="min-w-0 flex-1"
                aria-label={bound.label}
                value={field.state.value as string | null}
                defaultMonth={values[index === 0 ? 1 : 0]}
                invalid={invalid}
                required={bound.required}
                disabled={disabled}
                onChange={(next) => {
                  field.handleChange(next);
                  if (index === 0) onBeginChanged(next);
                }}
                onBlur={field.handleBlur}
              />
            )}
          </form.Field>
        </div>
      ))}
    </div>
  );
}
