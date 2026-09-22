"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { cn } from "@/lib/utils";
import type { TeamEventDetail } from "../../types";
import type { TeamEventEditValues } from "../team-event-edit-schema";

/**
 * Which occurrences of a series an edit touches — the inline "all / all future / only this event" radios
 * the legacy form showed once a recurring event was changed (`TeamEventPagesRest.createEditLayout`).
 *
 * Shown only when editing a *stored* recurring event: a new one or a non-recurring one has no series to
 * scope. The choice between "all" and "all future" follows the server's own rule — a later occurrence
 * offers FUTURE, the first offers ALL, both alongside SINGLE (the master start comes from the loaded
 * event, which the opened-occurrence prefill does not touch, see CalendarEditRouteClient). The server
 * refuses a save without this answer (`validate`), a refusal the schema anticipates so the radios are
 * marked required before the round-trip (see team-event-edit-schema.ts).
 */
export function SeriesModificationSection({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const master = useEntityData<TeamEventDetail>();

  const recurring = useStore(form.store, (state: unknown) => {
    const values = (state as { values: TeamEventEditValues }).values;
    return values.id != null && Boolean(values.recurrenceRule?.trim());
  });
  const occurrenceStart = useStore(form.store, (state: unknown) => {
    const values = (state as { values: TeamEventEditValues }).values;
    return values.selectedSeriesEvent?.startDate ?? values.startDate ?? null;
  });

  if (!recurring) return null;

  // A later occurrence can change "this and all future"; the first (or an unknown one) changes "all".
  const isLaterOccurrence =
    !!master?.startDate &&
    !!occurrenceStart &&
    master.startDate < occurrenceStart;
  const scoped = isLaterOccurrence ? "FUTURE" : "ALL";

  const options: { value: "ALL" | "FUTURE" | "SINGLE"; labelKey: string }[] = [
    {
      value: scoped,
      labelKey:
        scoped === "FUTURE"
          ? "plugins.teamcal.event.recurrence.change.future"
          : "plugins.teamcal.event.recurrence.change.all",
    },
    {
      value: "SINGLE",
      labelKey: "plugins.teamcal.event.recurrence.change.single",
    },
  ];

  return (
    <form.Field name={"seriesModificationMode" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const label = t("plugins.teamcal.event.recurrence.change.text");
        return (
          <FieldShell
            name="seriesModificationMode"
            label={label}
            required
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            ids={ids}
            className={cn(className)}
          >
            <RadioGroup
              value={field.state.value ?? ""}
              onValueChange={(v) => field.handleChange(v)}
              className="gap-2"
            >
              {options.map((opt) => (
                <div key={opt.value} className="flex items-center gap-2">
                  <RadioGroupItem
                    id={`series-mode-${opt.value}`}
                    value={opt.value}
                  />
                  <Label
                    htmlFor={`series-mode-${opt.value}`}
                    className="text-sm font-normal text-foreground"
                  >
                    {t(opt.labelKey)}
                  </Label>
                </div>
              ))}
            </RadioGroup>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
