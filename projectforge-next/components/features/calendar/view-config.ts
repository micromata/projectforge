/**
 * Pure FullCalendar configuration derived from the user's filter — no React, no i18n, so it is unit
 * tested (see `view-config.test.ts`). The i18n button labels and the click handlers are added on top in
 * `use-view-buttons.ts`.
 */

import { addMonths, parseISO, startOfMonth } from "date-fns";
import type { CalendarOptions } from "@fullcalendar/core";
import type { CalendarViewKey } from "@/lib/rs/calendar-types";

/** Longest range `POST /rs/calendar/events` serves before a `BadRequestException` (CalendarServicesRest). */
export const MAX_EVENT_RANGE_DAYS = 50;

const DAY_MS = 24 * 60 * 60 * 1000;
const MONTH_VIEWS: ReadonlySet<string> = new Set([
  "dayGridMonth",
  "dayGridWorkingMonth",
]);

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

/** FullCalendar `slotDuration` for a grid size in minutes: 30 → "00:30:00", 60 → "01:00:00", 5 → "00:05:00". */
export function slotDuration(gridSize: number): string {
  const size =
    Number.isFinite(gridSize) && gridSize > 0 ? Math.trunc(gridSize) : 30;
  return `${pad(Math.floor(size / 60))}:${pad(size % 60)}:00`;
}

/** FullCalendar `scrollTime` for the first hour to show (clamped 0..23): 8 → "08:00:00". */
export function scrollTime(firstHour: number): string {
  const hour = Number.isFinite(firstHour)
    ? Math.min(23, Math.max(0, Math.trunc(firstHour)))
    : 8;
  return `${pad(hour)}:00:00`;
}

/**
 * The date FullCalendar should open at. In a month view `init.date` is the first *visible* day, which
 * belongs to the previous month whenever the month does not start on the week's first day — passing it
 * verbatim would open that previous month. Snap to the first of the intended month (the month after the
 * leading day), which is exactly what the legacy panel did.
 */
export function normalizeInitialDate(
  date: string | null | undefined,
  view: CalendarViewKey | null | undefined
): Date | undefined {
  if (!date) return undefined;
  const parsed = parseISO(date);
  if (Number.isNaN(parsed.getTime())) return undefined;
  if (view && MONTH_VIEWS.has(view) && parsed.getDate() !== 1) {
    return startOfMonth(addMonths(parsed, 1));
  }
  return parsed;
}

/**
 * Clamps a visible range's end so it never exceeds {@link MAX_EVENT_RANGE_DAYS} from its start — an
 * unusual `firstDay` combined with `listMonth` can otherwise span wider than `POST /events` accepts.
 */
export function clampVisibleEnd(start: Date, end: Date): Date {
  const maxEnd = new Date(start.getTime() + MAX_EVENT_RANGE_DAYS * DAY_MS);
  return end.getTime() > maxEnd.getTime() ? maxEnd : end;
}

/**
 * Button layout of the calendar's own header; the labels come from `use-view-buttons.ts`. The "new
 * entry" button is not here: it lives in the page header as the shared AddEntryButton (see
 * CalendarPage), so it carries the same icon, tooltip and `N`/`+` shortcut as every list page.
 */
export const HEADER_TOOLBAR: CalendarOptions["headerToolbar"] = {
  left: "title",
  center:
    "dayGridMonth,dayGridWorkingMonth,listMonth timeGridWeek,timeGridWorkingWeek,dayGridWeek,listWeek timeGridDay",
  right: "today prev,next",
};

/** Per-view labels for the header buttons (resolved from i18n in `use-view-buttons.ts`). */
export interface ViewLabels {
  month: string;
  week: string;
  day: string;
  /** The "5/7" working-days variant. */
  workDays: string;
  /** `dayGridWeek` — a week at a glance. */
  overview: string;
  /** The list views. */
  agenda: string;
}

/**
 * The `views` map, including the two weekend-less "working" variants the backend can send and
 * FullCalendar does not know out of the box. Time-grid views get their slot size and scroll position
 * from the current filter, month views drop the trailing empty week.
 */
export function buildViews(opts: {
  gridSize: number;
  firstHour: number;
  hour12?: boolean;
  labels: ViewLabels;
}): CalendarOptions["views"] {
  const timeGrid = {
    slotDuration: slotDuration(opts.gridSize),
    scrollTime: scrollTime(opts.firstHour),
    slotEventOverlap: false,
  };
  return {
    dayGrid: {
      titleFormat: { year: "numeric", month: "long", day: "2-digit" },
    },
    timeGrid: {
      titleFormat: { year: "numeric", month: "long", day: "2-digit" },
      slotLabelFormat: {
        hour: "2-digit",
        minute: "2-digit",
        hour12: opts.hour12,
      },
    },
    dayGridMonth: { fixedWeekCount: false, buttonText: opts.labels.month },
    dayGridWorkingMonth: {
      type: "dayGridMonth",
      weekends: false,
      fixedWeekCount: false,
      buttonText: opts.labels.workDays,
    },
    timeGridWeek: { ...timeGrid, buttonText: opts.labels.week },
    timeGridWorkingWeek: {
      type: "timeGridWeek",
      weekends: false,
      ...timeGrid,
      buttonText: opts.labels.workDays,
    },
    timeGridDay: { ...timeGrid, buttonText: opts.labels.day },
    dayGridWeek: { buttonText: opts.labels.overview },
    listMonth: { buttonText: opts.labels.agenda },
    listWeek: { buttonText: opts.labels.agenda },
  };
}
