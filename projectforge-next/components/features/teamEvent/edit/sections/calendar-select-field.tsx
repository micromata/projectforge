"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { fetchTeamCalendars } from "@/lib/rs/team-event";

/** The React Query key of the calendar options — one list per session, so this is a cache read after the first form. */
const CALENDARS_QUERY_KEY = ["teamEvent", "calendars"] as const;

/**
 * Which calendar the event lives in — the `calendar` select of the legacy form.
 *
 * Its own component rather than the shared [SelectField], for two reasons the generic one cannot serve:
 * the options are not a fixed enum but the writable calendars fetched from the backend
 * (`TeamEventPagesRest.getCalendars`), and the field is mandatory though `TeamEventDO` does not mark it
 * so — an event cannot be saved into no calendar. The shared select reads `required` from the metadata,
 * which for a relation the generator does not carry would be "optional" and offer a clear button; here
 * it is required and cannot be cleared.
 *
 * The form value is `{id, displayName}` like every other reference (see team-event-edit-schema.ts); only
 * the id is written back, and Spring resolves the calendar object from it.
 */
export function CalendarSelectField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const label = t("plugins.teamcal.event.teamCal");

  const { data: calendars = [] } = useQuery({
    queryKey: CALENDARS_QUERY_KEY,
    queryFn: ({ signal }) => fetchTeamCalendars(signal),
    staleTime: Infinity,
  });

  return (
    <form.Field name={"calendar" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const value = field.state.value as {
          id: number;
          displayName?: string;
        } | null;
        // The calendars this user may write into, plus the event's own if it is not among them: an event
        // can live in a calendar the user only reads (a shared one), and dropping it from the list would
        // silently move the event on the next save. Mirrors the legacy createEditLayout.
        const options = calendars.map((cal) => ({
          id: cal.id,
          title: cal.title,
        }));
        if (value && !options.some((o) => o.id === value.id)) {
          options.unshift({
            id: value.id,
            title: value.displayName || String(value.id),
          });
        }
        return (
          <FieldShell
            name="calendar"
            label={label}
            required
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <Select
              value={value == null ? "" : String(value.id)}
              onValueChange={(v) => {
                if (v === "") return;
                const chosen = options.find((o) => String(o.id) === v);
                field.handleChange({
                  id: Number(v),
                  displayName: chosen?.title ?? v,
                });
              }}
            >
              <SelectTrigger
                id={ids.controlId}
                aria-labelledby={ids.labelId}
                className="w-full"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {options.map((o) => (
                  <SelectItem key={o.id} value={String(o.id)}>
                    {o.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
