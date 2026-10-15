"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { DateTimeInput } from "@/components/shared/date-time-input";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { TimeInput } from "@/components/shared/time-input";
import { useFormatContext } from "@/hooks/use-format";
import { leafKeyOf } from "@/lib/leaf-key";
import { zonedIsoOf, zonedPartsOf } from "@/lib/user-zone";
import { cn } from "@/lib/utils";
import type { FieldMetaState } from "@/components/shared/form/field-shell";
import {
  durationMinutesOf,
  normalizedStopTime,
  stopTimeForNewStart,
} from "../day-range";
import type { TimesheetEditValues } from "../timesheet-edit-schema";

/**
 * When the work happened: the two ends of the sheet and the length between them — the `dayRange` widget
 * of the legacy form.
 *
 * One block rather than two fields, because the two ends are not independent. Moving the start moves the
 * stop with it, keeping the length (a correction of *when*, not of *how long*); a stop that lands at or
 * before the start is read as the next day's, so 08:00–00:30 is a sheet that runs past midnight. Both
 * rules are pure and live in `../day-range.ts`, where they are tested.
 *
 * Only the start carries a date. A sheet is one span of one working day and runs at most ten hours, so
 * the stop is a time of day alone: it hangs on the start's date, and a stop at or before the start is the
 * next day's (`sameDate` on the legacy `TimeRange`, `s. Bestandsanwendung /react`). Its wire value stays
 * a full instant — the backend field is unchanged; the user just never picks its date.
 *
 * The duration is shown, not edited. Wicket offered it as a third box that wrote the stop; here the two
 * ends are what the entity has, and the length is what they say.
 *
 * The user the sheet is booked for shares this row: who and when belong together and each is narrow, so
 * one wrapping line holds all four (user, start, stop, length) instead of spending a full row on the
 * user alone. The user field is declared here rather than on the page for that reason.
 */
export function DayRangeSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ctx = useFormatContext();
  const startIds = useFieldIds();
  const stopIds = useFieldIds();

  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as TimesheetEditValues
  );
  const minutes = durationMinutesOf(values.startTime, values.stopTime);

  return (
    <div
      className={cn(
        // A wrapping row at every width, not a full-width column when narrow: the three time boxes keep
        // their own compact widths (a date-and-time, a time, an h:mm) and wrap together as a group, so a
        // narrow window stacks them into neat pairs rather than stretching each to the full line. The user
        // (w-full) always claims its own line; the rest fall in beside it or below as the width allows.
        "flex flex-wrap items-start gap-x-4 gap-y-4",
        className
      )}
    >
      <EntityAutocompleteField
        name="user"
        // "user" is both a text and a namespace in the bundle, so resolve to its exported leaf (see leafKeyOf).
        label={t(leafKeyOf("user", t.has))}
        entity="user"
        // A bounded width, not flex-1: a name needs about this much and letting it grow into the row's
        // leftover space made it far wider than it reads.
        className="w-full min-w-0 md:w-64"
      />
      <form.Field name={"startTime" as never}>
        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
        {(field: any) => {
          const meta = field.state.meta as FieldMetaState;
          return (
            <FieldShell
              name="startTime"
              label={t("timesheet.startTime")}
              required
              invalid={meta.isTouched && !meta.isValid}
              errors={fieldErrors(meta, t("timesheet.startTime"))}
              ids={startIds}
              // Its own content width at every breakpoint, so it never stretches to the full line: on a
              // narrow window it wraps to a new line at this compact size and pairs with the stop box.
              className="w-auto"
            >
              <DateTimeInput
                value={field.state.value as string | null}
                dateLabel={t("timesheet.startTime")}
                timeLabel={t("timesheet.startTime")}
                onChange={(next) => {
                  // Read before writing: the held length is the one the *old* pair had.
                  const stop = stopTimeForNewStart(next, {
                    startTime: values.startTime,
                    stopTime: values.stopTime,
                  });
                  field.handleChange(next);
                  form.setFieldValue("stopTime", stop);
                }}
              />
            </FieldShell>
          );
        }}
      </form.Field>
      <form.Field name={"stopTime" as never}>
        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
        {(field: any) => {
          const meta = field.state.meta as FieldMetaState;
          return (
            <FieldShell
              name="stopTime"
              label={t("timesheet.stopTime")}
              required
              invalid={meta.isTouched && !meta.isValid}
              errors={fieldErrors(meta, t("timesheet.stopTime"))}
              ids={stopIds}
              // An explicit width at every breakpoint: the field only holds a time of day, but the Field's
              // `*:w-full` on its children defeats the TimeInput's own width, so without a bound here it
              // stretches to the full line — a huge box for an `hh:mm` — instead of staying compact.
              className="w-28"
            >
              <TimeInput
                id={stopIds.controlId}
                aria-label={t("timesheet.stopTime")}
                // No start, no day to hang the end on — the field waits for one.
                disabled={!values.startTime}
                value={
                  zonedPartsOf(field.state.value as string | null, ctx)?.time
                }
                onChange={(time) => {
                  if (!time) {
                    field.handleChange(null);
                    return;
                  }
                  const startDate = zonedPartsOf(values.startTime, ctx)?.date;
                  if (!startDate) return;
                  // The time typed, hung on the start's date, then rolled to the next day when it
                  // lands at or before the start (normalizedStopTime).
                  const iso = zonedIsoOf(startDate, time, ctx);
                  field.handleChange(normalizedStopTime(iso, values.startTime));
                }}
                className={cn(ctx.hour12 ? "w-[8.5rem]" : "w-[6rem]")}
              />
            </FieldShell>
          );
        }}
      </form.Field>
      <div className="flex w-auto flex-col gap-1.5">
        <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {t("timesheet.duration")}
        </span>
        {/* A dash while either end is missing, so the line keeps its height and reads as "not yet". */}
        <span className="text-sm font-medium tabular-nums">
          {minutes != null && minutes > 0 ? formatDuration(minutes) : "–"}
        </span>
      </div>
    </div>
  );
}

/**
 * A length as `h:mm`, the form the legacy page showed it in.
 *
 * Not through `lib/format.ts`: this is a duration and not a time of day, so it has no zone and no locale
 * to be in — `7:30` is seven and a half hours in every one of them, and 25 hours stays `25:00` rather
 * than becoming a day.
 */
function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  return `${hours}:${String(minutes % 60).padStart(2, "0")}`;
}
