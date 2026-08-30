/**
 * The wire contract of the calendar REST controllers, mirrored field-for-field from
 * `org.projectforge.rest.calendar` (`CalendarServicesRest`, `CalendarFilterServicesRest`) and the
 * business DTOs they serialise.
 *
 * No field is renamed on the wire (no `@JsonProperty` on any of these), so these names are verbatim.
 * Jackson runs with `JsonInclude.NON_NULL` globally, so an absent key means "unchanged / not set",
 * never `null` — which is exactly why the partial patches ({@link CalendarInitPatch}) merge cleanly.
 * Non-null Kotlin primitives (`gridSize`, `firstHour`, `editable`, `visible`, `isFilterModified`, …)
 * are always present.
 */

/** FullCalendar view keys the backend emits and accepts (`CalendarView.key`). */
export type CalendarViewKey =
  | "dayGridMonth"
  | "dayGridWorkingMonth"
  | "timeGridWeek"
  | "timeGridWorkingWeek"
  | "timeGridDay"
  | "dayGridWeek"
  | "listWeek"
  | "listMonth";

/** Grid sizes `changeGridSize` accepts, in minutes (`CalendarFilterServicesRest`). */
export const CALENDAR_GRID_SIZES = [5, 10, 15, 30, 60] as const;
export type CalendarGridSize = (typeof CALENDAR_GRID_SIZES)[number];

/** How much of a calendar the current user may see (`TeamCalendar.ACCESS`). */
export type CalendarAccess = "OWNER" | "FULL" | "READ" | "MINIMAL" | "NONE";

/** The single serialised field of `CalendarStyle` — there is no `fgColor` (text/bg are computed). */
export interface CalendarStyle {
  bgColor: string;
}

/** `CalendarStyleMap`: styles keyed by calendar id (as a string), wrapped in a `styles` object. */
export interface CalendarStyleMap {
  styles: Record<string, CalendarStyle>;
}

/** A calendar in `listOfDefaultCalendars` — the pseudo "timesheets" calendar has id `-1`. */
export interface TeamCalendar {
  id: number | null;
  title: string | null;
  access?: CalendarAccess | null;
  externalSubscription: boolean;
}

/** A calendar in `teamCalendars` / `activeCalendars`: a {@link TeamCalendar} with style and visibility. */
export interface StyledTeamCalendar extends TeamCalendar {
  style?: CalendarStyle | null;
  visible: boolean;
}

/** A minimal user reference (`User.copyFromMinimal`). */
export interface UserRef {
  id?: number | null;
  username?: string | null;
  displayName?: string | null;
}

/** A minimal group reference (`Group.copyFromMinimal`). */
export interface GroupRef {
  id?: number | null;
  name?: string | null;
  displayName?: string | null;
}

/** A saved calendar filter, as offered in the favourites menu (`Favorites.FavoriteIdTitle`). */
export interface CalendarFilterFavorite {
  id: number;
  name: string;
}

/**
 * The persisted filter (`org.projectforge.business.calendar.CalendarFilter`). `view` and the visible
 * date do NOT live here — they are on {@link CalendarInit}. `gridSize`, `firstHour` and
 * `otherTimesheetUsersEnabled` are non-null on the wire.
 */
export interface CalendarFilter {
  name?: string | null;
  id?: number | null;
  defaultCalendarId?: number | null;
  gridSize: number;
  firstHour: number;
  otherTimesheetUsersEnabled: boolean;
  timesheetUserId?: number | null;
  vacationGroupIds?: number[] | null;
  vacationUserIds?: number[] | null;
  showBreaks?: boolean | null;
  showPlanning?: boolean | null;
  calendarIds?: number[];
  invisibleCalendars?: number[];
}

/**
 * The full page state (`GET /rs/calendar/initial`, also the answer of `selectFilter`).
 * `translations` is built for the legacy client and ignored here.
 */
export interface CalendarInit {
  date?: string | null;
  view?: CalendarViewKey | null;
  alternateHoursBackground?: boolean | null;
  teamCalendars?: StyledTeamCalendar[] | null;
  filterFavorites?: CalendarFilterFavorite[] | null;
  filter?: CalendarFilter | null;
  timesheetUser?: UserRef | null;
  activeCalendars?: StyledTeamCalendar[] | null;
  vacationGroups?: GroupRef[] | null;
  vacationUsers?: UserRef[] | null;
  listOfDefaultCalendars?: TeamCalendar[] | null;
  styleMap?: CalendarStyleMap | null;
  translations?: Record<string, string> | null;
  isFilterModified: boolean;
}

/**
 * What every mutating `change*` / favourite endpoint answers: a subset of {@link CalendarInit} keys
 * (`selectFilter` is the exception — it returns a whole `CalendarInit`). Which keys are present varies
 * per endpoint (e.g. `changeStyle` omits `isFilterModified`; the `change*` toggles return only
 * `isFilterModified`), so every key is optional and merged over the cached init.
 */
export type CalendarInitPatch = Partial<
  Pick<
    CalendarInit,
    | "filter"
    | "activeCalendars"
    | "teamCalendars"
    | "styleMap"
    | "filterFavorites"
    | "isFilterModified"
  >
>;

/** How event colours are derived (`CalendarEventColorScheme`): transparent standard vs. higher-contrast classic. */
export type CalendarEventColorScheme = "STANDARD" | "CLASSIC";

/**
 * The calendar's presentational settings, persisted separately from the filter under the user-pref area
 * `calendar` (`org.projectforge.rest.calendar.CalendarSettings`, via `GET`/`POST /rs/calendarSettings/settings`).
 * The four colours are hex strings; the backend fills any missing one with its default on read.
 */
export interface CalendarSettings {
  timesheetsColor?: string | null;
  timesheetsBreaksColor?: string | null;
  timesheetsStatsColor?: string | null;
  vacationsColor?: string | null;
  colorScheme?: CalendarEventColorScheme | null;
  alternateHoursBackground?: boolean | null;
}

/** A tooltip carried on an event (`FullCalendarEvent.Tooltip`); `text` is HTML. */
export interface CalendarEventTooltip {
  title?: string | null;
  text: string;
}

/**
 * The non-FullCalendar-native payload of an event. `category` decides where a click navigates
 * (`address | timesheet | timesheet-break | timesheet-stats | vacation | calEvent | teamEvent |
 * holiday`); `dbId`/`uid` identify the entity behind it.
 */
export interface CalendarEventExtendedProps {
  duration?: string | null;
  /**
   * A time-grid event's extra line under the title. Serialised at the event's top level, but FullCalendar
   * moves every non-native field into `extendedProps`, so that is where it is read (see full-calendar-panel).
   */
  description?: string | null;
  uid?: string | null;
  dbId?: number | null;
  tooltip?: CalendarEventTooltip | null;
  category?: string | null;
  /**
   * Preformatted AI time savings of a time sheet, e.g. "1:00h, 33 %" (`AITimeSavings`). Present only when
   * a saving is given and non-zero (see TimesheetEventsProvider); shown as a compact line in the block.
   */
  timeSavedByAI?: string | null;
  /**
   * Whether this is one occurrence of a recurring team event (`TeamCalEventsProvider`). A click on such
   * an event carries the clicked occurrence's date so a single/future edit knows which day it acts on
   * (see use-calendar-action.ts); absent or false for a one-off event.
   */
  recurrence?: boolean | null;
}

/**
 * One event as FullCalendar consumes it (`FullCalendarEvent`). `start`/`end` are a single string —
 * either `yyyy-MM-dd` (all-day/background) or an ISO instant — thanks to `EventDateSerializer`; pass
 * them to FullCalendar verbatim, never re-parse (that would shift all-day events by the tz offset).
 */
export interface FullCalendarEventDto {
  id?: string | null;
  title?: string | null;
  description?: string | null;
  allDay?: boolean | null;
  classNames?: string | null;
  editable: boolean;
  startEditable?: boolean | null;
  durationEditable?: boolean | null;
  start?: string | null;
  end?: string | null;
  extendedProps?: CalendarEventExtendedProps | null;
  overlap?: boolean | null;
  /** `"background"` for weekend/holiday shading. */
  display?: string | null;
  textColor?: string | null;
  backgroundColor?: string | null;
  borderColor?: string | null;
}

/** The `POST /rs/calendar/events` response (`CalendarServicesRest.CalendarData`). */
export interface CalendarData {
  date: string;
  alternateHoursBackground?: boolean | null;
  events: FullCalendarEventDto[];
}

/**
 * The `POST /rs/calendar/events` request (`CalendarRestFilter`). `start` is required (a 400 otherwise);
 * the range may not exceed 50 days. `vacationGroupIds`/`vacationUserIds` sent here are ignored — the
 * server takes them from the persisted filter — so they belong in the query key, not this body.
 */
export interface CalendarEventsFilter {
  start: string;
  end?: string;
  view?: CalendarViewKey;
  timesheetUserId?: number | null;
  showBreaks?: boolean | null;
  activeCalendarIds?: number[];
  useVisibilityState?: boolean;
  timeZone?: string;
}

/** The `POST /rs/calendar/storeState` request (`CalendarServicesRest.CalendarState`). */
export interface CalendarState {
  date?: string | null;
  view?: string | null;
  timeZone?: string | null;
  activeCalendars?: StyledTeamCalendar[] | null;
}

/**
 * The query parameters of `GET /rs/calendar/action` — a slot select, event create, resize or
 * drag-and-drop. The backend answers with a `ResponseAction` whose `url` is the edit page to open
 * (e.g. `/timesheet/edit?startDate=…`, `/teamEvent/edit?…&calendar=<id>`). There is no `view` or
 * `calendarId` parameter; the target calendar id travels inside the produced url.
 */
export interface CalendarActionParams {
  action: "slotSelected" | "create" | "resize" | "dragAndDrop";
  startDate?: string;
  endDate?: string;
  category?: string;
  dbId?: string;
  uid?: string;
  origStartDate?: string;
  origEndDate?: string;
  firstHour?: number;
}

/** The `GET /rs/calendar/refresh` answer: whether the client should reload its subscriptions. */
export interface CalendarRefreshResult {
  reload: boolean;
}
