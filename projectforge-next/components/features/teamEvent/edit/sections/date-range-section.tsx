"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { DateInput } from "@/components/shared/date-input";
import { DateTimeInput } from "@/components/shared/date-time-input";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFormatContext } from "@/hooks/use-format";
import { cn } from "@/lib/utils";
import {
  DEFAULT_FROM_TIME,
  DEFAULT_TO_TIME,
  zonedIsoOf,
  zonedPartsOf,
} from "@/lib/user-zone";
import type { FormatContext } from "@/lib/format";
import type { TeamEventEditValues } from "../team-event-edit-schema";

/**
 * When the event happens: its two ends and whether it lasts the whole day — the `startDate`, `endDate`
 * and `allDay` fields of the legacy form in one block.
 *
 * One block rather than three fields, because the all-day switch changes what the other two are. On it,
 * the two ends are dates (a day, or a span of days) and the time of day is not the user's to set; off it,
 * they are instants entered to the minute. Toggling it re-anchors both ends to the day they fall on —
 * the start to its first minute, the end to its last — so a timed 14:00–15:30 becomes the whole of that
 * day rather than keeping a time the field no longer shows.
 */
export function DateRangeSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const ctx = useFormatContext();

  const allDay = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TeamEventEditValues).allDay === true
  );

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-2",
        className
      )}
    >
      <EventDateField
        name="startDate"
        label={t("plugins.teamcal.event.beginDate")}
        fallbackTime={DEFAULT_FROM_TIME}
        allDay={allDay}
      />
      <EventDateField
        name="endDate"
        label={t("plugins.teamcal.event.endDate")}
        fallbackTime={DEFAULT_TO_TIME}
        allDay={allDay}
      />
      <form.Field name={"allDay" as never}>
        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
        {(field: any) => (
          <div className="flex items-center gap-2 md:col-span-2">
            <Checkbox
              id="teamEvent-allDay"
              checked={field.state.value === true}
              onCheckedChange={(value) => {
                const next = value === true;
                field.handleChange(next);
                // Re-anchor both ends to the day they fall on, so the hidden time of an all-day event
                // is its boundaries rather than whatever a timed one happened to carry.
                if (next) {
                  const values = form.state.values as TeamEventEditValues;
                  form.setFieldValue(
                    "startDate",
                    reanchor(values.startDate, ctx, DEFAULT_FROM_TIME)
                  );
                  form.setFieldValue(
                    "endDate",
                    reanchor(values.endDate, ctx, DEFAULT_TO_TIME)
                  );
                }
              }}
              onBlur={field.handleBlur}
            />
            <Label
              htmlFor="teamEvent-allDay"
              className="text-xs font-normal text-foreground"
            >
              {t("plugins.teamcal.event.allDay")}
            </Label>
          </div>
        )}
      </form.Field>
    </div>
  );
}

/** The instant of the given end re-read at the fixed time of day of its own day, or null while it is empty. */
function reanchor(
  iso: string | null,
  ctx: FormatContext,
  time: string
): string | null {
  const parts = zonedPartsOf(iso, ctx);
  if (!parts) return null;
  return zonedIsoOf(parts.date, time, ctx, time);
}

/**
 * One end of the event, entered as a date alone while it lasts all day and as a date plus a time of day
 * otherwise. The form value is the ISO instant either way; the date-only mode anchors it to the fixed
 * time of day this end falls on (midnight for the start, the last minute for the end).
 */
function EventDateField({
  name,
  label,
  fallbackTime,
  allDay,
}: {
  name: "startDate" | "endDate";
  label: string;
  fallbackTime: string;
  allDay: boolean;
}) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ctx = useFormatContext();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const iso = field.state.value as string | null;
        return (
          <FieldShell
            name={name}
            label={label}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            ids={ids}
          >
            {allDay ? (
              <DateInput
                id={ids.controlId}
                aria-label={label}
                value={zonedPartsOf(iso, ctx)?.date ?? null}
                onChange={(date) =>
                  field.handleChange(
                    date
                      ? zonedIsoOf(date, fallbackTime, ctx, fallbackTime)
                      : null
                  )
                }
              />
            ) : (
              <DateTimeInput
                value={iso}
                dateLabel={label}
                timeLabel={label}
                fallbackTime={fallbackTime}
                onChange={(next) => field.handleChange(next)}
              />
            )}
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
