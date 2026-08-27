import { TEAM_EVENT_METADATA } from "@/lib/metadata/team-event.generated";
import { definePage } from "@/lib/page-def/define-page";
import { CalendarSelectField } from "./edit/sections/calendar-select-field";
import { DateRangeSection } from "./edit/sections/date-range-section";
import { RecurrenceSection } from "./edit/sections/recurrence-section";
import { ReminderSection } from "./edit/sections/reminder-section";
import { SeriesModificationSection } from "./edit/sections/series-modification-section";
import {
  teamEventEditSchema,
  TEAM_EVENT_EDIT_FIELDS,
  type TeamEventEditValues,
} from "./edit/team-event-edit-schema";
import {
  emptyTeamEventValues,
  toFormValues,
} from "./edit/team-event-edit-values";
import type { TeamEventDetail, TeamEventListRow } from "./types";

/** REST category of a team event — the entity name every shared hook is parameterised with. */
export const TEAM_EVENT_ENTITY = "teamEvent";
/** React Query key of the list, so a write from the edit page refreshes it once the list is built. */
export const TEAM_EVENT_LIST_QUERY_KEY = ["teamEvent"] as const;
/** Where the calendar's slot-select preset reads from (see TeamEventPagesRest.newBaseDTO). */
const NEW_ENTRY_PARAMS = ["startDate", "endDate", "calendar"] as const;

/**
 * The whole team event page as data (see lib/page-def/types.ts).
 *
 * The list is declared but not routed: a team event is reached through the calendar, which opens this
 * edit page directly (see use-calendar-action.ts) — `columns` names the entity's own fields in the one
 * shape every page has, so adding a list later is a route and not a restructuring (see TeamEventListRow).
 *
 * The edit page is the one that is live. Its fields follow the legacy form
 * (`TeamEventPagesRest.createEditLayout`) — the calendar the event lives in, its subject, the period, the
 * two texts, the reminder and the recurrence. Reminder and recurrence are each their own control the
 * UILayout renderer expressed as a `UICustomized`, hand-built here on the DTO fields they carry (see the
 * `reminder-section`, `recurrence-section` and `series-modification-section`). Editing a stored recurring
 * event asks which occurrences the change touches, the same choice the server enforces. Attendees remain
 * out of this phase: an event that has them still round-trips untouched (see team-event-edit-schema.ts).
 */
export const TEAM_EVENT_PAGE = definePage<
  TeamEventListRow,
  TeamEventEditValues,
  TeamEventDetail,
  typeof TEAM_EVENT_METADATA
>({
  entity: TEAM_EVENT_ENTITY,
  metadata: TEAM_EVENT_METADATA,
  route: "/teamEvent",
  queryKey: TEAM_EVENT_LIST_QUERY_KEY,
  // Calendar (MenuItemDefId.CALENDAR) — the event has no list menu entry of its own; it lives in the
  // calendar.
  categoryKey: "menu.calendar",
  titleKey: "plugins.teamcal.event.title.list",
  // Minimal, since no list renders yet: the fields that identify an event, in the order the legacy list
  // shows them (`TeamEventPagesRest.createListLayout` only shows the subject).
  columns: [
    { name: "subject", size: 260, className: "font-medium" },
    { name: "startDate", size: 150 },
    { name: "endDate", size: 150 },
    { name: "location", size: 160 },
  ],
  edit: {
    schema: teamEventEditSchema,
    fieldNames: TEAM_EVENT_EDIT_FIELDS,
    defaultValues: emptyTeamEventValues,
    toFormValues,
    title: (event) => event.subject ?? "",
    newTitleKey: "plugins.teamcal.event.title.add",
    savedMessageKey: "message.successfullChanged",
    newEntryParams: NEW_ENTRY_PARAMS,
    // Offer the clone, as Wicket does (TeamEventPagesRest.cloneSupport). `cloneData` prepares it (id and
    // timestamps dropped, the default prepareClone; AUTOSAVE is not honoured there, only NONE turns
    // clone off, see AbstractEntityRest.cloneData); the add form opens under `/teamEvent/new?clone=1`.
    clone: true,
    // "In Zeitbuchung umwandeln" — build a time sheet from this event's span and texts and open it as a
    // new sheet (TeamEventPagesRest.switch2Timesheet → TimesheetPagesRest.cloneFromCalendarEvent). The
    // time sheet is named, not imported, so the two features don't depend on each other in a circle.
    convert: {
      action: "switch2Timesheet",
      targetEntity: "timesheet",
      targetRoute: "/timesheet",
      labelKey: "plugins.teamcal.switchToTimesheetButton",
    },
    // "Unwiderruflich löschen" — a team event is one of the few entities whose DAO allows the destroying
    // delete (TeamEventDao.isForceDeletionSupport; that flag isn't serialized, so it's opted in here).
    forceDelete: true,
    // Save and cancel come back to the calendar, which is the only thing that opens the form — there is
    // no team event list of this app to return to (see toTeamEventRoute).
    returnTargets: [{ route: "/calendar", labelKey: "menu.calendar" }],
    sections: [
      {
        id: "general",
        titleKey: "plugins.teamcal.event.title.heading",
        fields: [
          // Which calendar the event lives in — a fetched list, mandatory, its own control (see
          // CalendarSelectField).
          { custom: CalendarSelectField },
          // What the event is called; highlighted and focused, as the legacy form focused it.
          { name: "subject", span: 2, emphasized: true },
          // Start, end and the all-day switch that changes what the two ends are (see DateRangeSection).
          { custom: DateRangeSection, span: 3 },
          { name: "location" },
          { name: "note", rows: 4, span: 3 },
        ],
      },
      {
        id: "recurrence",
        titleKey: "plugins.teamcal.event.recurrence",
        fields: [
          // How the event repeats (see RecurrenceSection), and — for a stored recurring event — which
          // occurrences an edit touches (see SeriesModificationSection, which renders nothing otherwise).
          { custom: RecurrenceSection, span: 3 },
          { custom: SeriesModificationSection, span: 3 },
        ],
      },
      {
        id: "reminder",
        titleKey: "plugins.teamcal.event.reminder.title",
        fields: [
          // When to remind before the event begins (see ReminderSection).
          { custom: ReminderSection, span: 3 },
        ],
      },
    ],
  },
});
