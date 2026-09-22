// Mirrors org.projectforge.rest.dto.TeamEvent (projectforge-rest). Keep field names in sync with the
// Spring DTO: it is what `saveorupdate` reads back, so a name that differs here is a value silently
// dropped on the way in.

/**
 * A team calendar as the DTO carries it: `TeamCalDO`, of which only the id is written back and the
 * title is what names it. The form holds it as an `{id, displayName}` reference like every other one
 * (see team-event-edit-values.ts), so this is only the shape the backend sends and receives.
 */
export type CalendarRef = { id: number; title?: string | null };

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
 * (JacksonConfiguration), so an empty field is absent from the JSON rather than null — toFormValues
 * normalises it.
 *
 * The fields below `note` are not edited in this phase, but they are carried through the form untouched:
 * a hand-built form posts its values *as* the DTO (see EntityEditPage.save), so a field left out of the
 * schema would be dropped on save — which for a recurring event, an event with attendees or a reminder
 * would be silent data loss. Their editing UI is a later phase; their round trip is not optional.
 */
export interface TeamEventDetail {
  /** null for an event that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /**
   * Which occurrences of a series an edit touches, and the one the user opened — neither is stored on
   * the event; they are the client's answer to the "change all / future / only this" question, posted as
   * transient attributes the DAO reads (see team-event-edit-schema.ts and TeamEventPagesRest).
   */
  seriesModificationMode?: "ALL" | "FUTURE" | "SINGLE" | null;
  selectedSeriesEvent?: {
    startDate?: string | null;
    endDate?: string | null;
    allDay?: boolean | null;
  } | null;
  subject?: string | null;
  calendar?: CalendarRef | null;
  location?: string | null;
  note?: string | null;
  /**
   * Both ends as an instant, ISO 8601 in UTC ("2026-08-09T08:12:34.000Z") — a `java.util.Date` as
   * Jackson writes it. The user's wall clock is derived from it against their own time zone, never the
   * browser's (see DateTimeInput and lib/user-zone.ts). For an all-day event they are the day's
   * boundaries in the user's zone.
   */
  startDate?: string | null;
  endDate?: string | null;
  allDay?: boolean;
  // --- carried through untouched (see the class doc above) ---
  recurrenceRule?: string | null;
  recurrenceExDate?: string | null;
  recurrenceReferenceDate?: string | null;
  recurrenceReferenceId?: string | null;
  recurrenceUntil?: string | null;
  attendees?: unknown[] | null;
  reminderDuration?: number | null;
  reminderDurationUnit?: string | null;
  reminderActionType?: string | null;
  organizer?: string | null;
  organizerAdditionalParams?: string | null;
  ownership?: boolean | null;
  sequence?: number | null;
  uid?: string | null;
  dtStamp?: string | null;
  lastEmail?: string | null;
  attachments?: unknown[] | null;
  deleted?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/**
 * Projection of the (not yet built) list page.
 *
 * The list stays in the legacy calendar for now — a team event is reached through the calendar, which
 * opens this edit page directly (see use-calendar-action.ts). Declared so the page can name its columns
 * and be handed to `definePage` in the one shape every entity has; adding the list later is a route and
 * a column list, not a restructuring.
 */
export interface TeamEventListRow {
  id: number;
  subject?: string | null;
  calendar?: CalendarRef | null;
  startDate?: string | null;
  endDate?: string | null;
  location?: string | null;
}
