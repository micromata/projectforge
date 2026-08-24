"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { DateTimeInput } from "@/components/shared/date-time-input";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
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
 * The duration is shown, not edited. Wicket offered it as a third box that wrote the stop; here the two
 * ends are what the entity has, and the length is what they say.
 */
export function DayRangeSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
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
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
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
            >
              <DateTimeInput
                value={field.state.value as string | null}
                dateLabel={t("timesheet.stopTime")}
                timeLabel={t("timesheet.stopTime")}
                onChange={(next) =>
                  field.handleChange(normalizedStopTime(next, values.startTime))
                }
              />
            </FieldShell>
          );
        }}
      </form.Field>
      <div className="flex flex-col gap-1.5">
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
