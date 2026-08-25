import type { TeamEventEditValues } from "./team-event-edit-schema";
import type { CalendarRef, TeamEventDetail } from "../types";

/**
 * A calendar as the form holds it: `{id, displayName}` like every other reference, with the title the
 * backend sent as the name — that is what a calendar is called, and the DTO carries no `displayName` of
 * its own for it (see CalendarRef).
 */
export function toCalendarRef(
  calendar: CalendarRef | null | undefined
): TeamEventEditValues["calendar"] {
  if (!calendar) return null;
  return {
    ...calendar,
    displayName: calendar.title || String(calendar.id),
  };
}

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised to null here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value.
 *
 * `startDate`/`endDate` are handed on as they came: the wire format is already the ISO instant in UTC
 * that DateTimeInput consumes, and converting it into the user's zone is that input's business, not a
 * form value's (see lib/user-zone.ts).
 *
 * The fields below `allDay` are the ones the form does not edit but must not drop on save (see the
 * schema): they are copied over unchanged.
 */
export function toFormValues(event: TeamEventDetail): TeamEventEditValues {
  return {
    id: event.id ?? null,
    subject: event.subject ?? "",
    calendar: toCalendarRef(event.calendar),
    location: event.location ?? null,
    note: event.note ?? null,
    startDate: event.startDate ?? null,
    endDate: event.endDate ?? null,
    allDay: event.allDay ?? false,
    recurrenceRule: event.recurrenceRule ?? null,
    recurrenceExDate: event.recurrenceExDate ?? null,
    recurrenceReferenceDate: event.recurrenceReferenceDate ?? null,
    recurrenceReferenceId: event.recurrenceReferenceId ?? null,
    recurrenceUntil: event.recurrenceUntil ?? null,
    attendees: event.attendees ?? null,
    reminderDuration: event.reminderDuration ?? null,
    reminderDurationUnit: event.reminderDurationUnit ?? null,
    reminderActionType: event.reminderActionType ?? null,
    organizer: event.organizer ?? null,
    organizerAdditionalParams: event.organizerAdditionalParams ?? null,
    ownership: event.ownership ?? null,
    sequence: event.sequence ?? null,
    uid: event.uid ?? null,
    dtStamp: event.dtStamp ?? null,
    lastEmail: event.lastEmail ?? null,
    attachments: event.attachments ?? null,
    created: event.created ?? null,
    lastUpdate: event.lastUpdate ?? null,
  };
}

/**
 * Blank form for an event that doesn't exist yet, i.e. what is on screen for the moment before the
 * preset arrives.
 *
 * Deliberately empty of the values that matter: the calendar and the two ends of the period come from
 * `teamEvent/newEntry` — the backend presets the period from the calendar's parameters and the calendar
 * from the one the user clicked in (`TeamEventPagesRest.newBaseDTO` → `onBeforeGetItemAndLayout`).
 * Guessing any of them here would mean a form that briefly shows something else than what is being
 * edited.
 */
export function emptyTeamEventValues(): TeamEventEditValues {
  return {
    id: null,
    subject: "",
    calendar: null,
    location: null,
    note: null,
    startDate: null,
    endDate: null,
    allDay: false,
    recurrenceRule: null,
    recurrenceExDate: null,
    recurrenceReferenceDate: null,
    recurrenceReferenceId: null,
    recurrenceUntil: null,
    attendees: null,
    reminderDuration: null,
    reminderDurationUnit: null,
    reminderActionType: null,
    organizer: null,
    organizerAdditionalParams: null,
    ownership: null,
    sequence: null,
    uid: null,
    dtStamp: null,
    lastEmail: null,
    attachments: null,
    created: null,
    lastUpdate: null,
  };
}
