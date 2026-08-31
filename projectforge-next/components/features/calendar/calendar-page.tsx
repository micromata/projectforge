"use client";

import { useCallback, useMemo, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import type { CalendarApi } from "@fullcalendar/core";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { Skeleton } from "@/components/ui/skeleton";
import { useFormatContext } from "@/hooks/use-format";
import type { CalendarViewKey } from "@/lib/rs/calendar-types";
import { useCalendarInit } from "./use-calendar-init";
import { useCalendarEvents } from "./use-calendar-events";
import { useStoreCalendarState } from "./use-calendar-state";
import { useCalendarFilterMutations } from "./use-calendar-filter-mutations";
import { useCalendarAction } from "./use-calendar-action";
import { useCreateShortcut } from "./use-create-shortcut";
import { useGotoDate } from "./use-goto-date";
import { normalizeInitialDate } from "./view-config";
import { CalendarToolbar } from "./calendar-toolbar";
import { CalendarSelect } from "./calendar-select";
import { FullCalendarPanel } from "./full-calendar-panel";
import type { CalendarRange, EventsRequest } from "./types";

const ids = (list: { id: number | null }[]) =>
  list.map((c) => c.id).filter((id): id is number => id != null);

/**
 * The whole calendar page: the init query owns the filter and calendars, a local range (reported by the
 * panel on every navigation) owns the visible span, and the events query hangs off the two. The panel is
 * told nothing it can derive itself — it reports its range and renders the events it is handed.
 */
export function CalendarPage() {
  const t = useTranslations();
  const { timeZone } = useFormatContext();
  const { data: init, isPending, isError } = useCalendarInit();

  const [range, setRange] = useState<CalendarRange | null>(null);
  const [nonce, setNonce] = useState(0);
  const apiRef = useRef<CalendarApi | null>(null);
  const storeState = useStoreCalendarState();
  // Created once for the page and shared by the chooser row and the header toolbar (settings/refresh).
  const mutations = useCalendarFilterMutations();

  useGotoDate(
    apiRef,
    useCallback(() => setNonce((n) => n + 1), []),
    // The calendar has mounted and set `apiRef.current` once it has reported a range.
    range !== null
  );

  const activeCalendars = useMemo(
    () => init?.activeCalendars ?? [],
    [init?.activeCalendars]
  );
  const filter = init?.filter;
  const firstHour = filter?.firstHour ?? 8;

  // A new entry is a page-level action, not a FullCalendar toolbar button: the shared AddEntryButton
  // renders it in the header (see PageTitleRow below) so it matches every list page, and the same
  // `N` / `+` chord opens it. The backend picks timesheet vs. team event from the filter; the current
  // day comes from the calendar api the panel publishes to `apiRef`.
  const { requestAction } = useCalendarAction();
  const handleCreate = useCallback(() => {
    const api = apiRef.current;
    if (!api) return;
    void requestAction({
      action: "create",
      startDate: api.getDate().toISOString(),
      firstHour,
    });
  }, [requestAction, firstHour]);
  useCreateShortcut(handleCreate);

  const onRangeChange = useCallback(
    (next: CalendarRange) => {
      setRange(next);
      storeState({
        date: next.start,
        view: next.view,
        timeZone,
        activeCalendars,
      });
    },
    [storeState, timeZone, activeCalendars]
  );

  const request: EventsRequest | null = useMemo(
    () =>
      range
        ? {
            start: range.start,
            end: range.end,
            view: range.view,
            activeCalendarIds: ids(activeCalendars),
            timesheetUserId: filter?.timesheetUserId,
            showBreaks: filter?.showBreaks,
            vacationGroupIds: filter?.vacationGroupIds ?? [],
            vacationUserIds: filter?.vacationUserIds ?? [],
            nonce,
          }
        : null,
    [range, activeCalendars, filter, nonce]
  );

  const { data: eventsData } = useCalendarEvents(request, timeZone);

  if (isPending) {
    return (
      <div className="flex flex-1 flex-col gap-3 p-4">
        <Skeleton className="h-8 w-full" />
        <Skeleton className="flex-1" />
      </div>
    );
  }
  if (isError || !init) {
    return (
      <p className="p-4 text-sm text-destructive">{t("errorpage.title")}</p>
    );
  }

  return (
    <>
      {/* No category over the title: it would only repeat "Kalender". The chooser rides the title row's
          flexible middle (a wrapping field that grows down as more are picked) to save a vertical line. */}
      <PageTitleRow
        title={t("calendar.title")}
        legacyUrl="react/calendar"
        center={
          <CalendarSelect
            teamCalendars={init.teamCalendars ?? []}
            activeCalendars={init.activeCalendars ?? []}
            onSetActive={mutations.setActiveCalendars}
            onSetVisibility={mutations.setVisibility}
            onChangeStyle={mutations.changeStyle}
          />
        }
      >
        <AddEntryButton onClick={handleCreate} />
        <CalendarToolbar init={init} mutations={mutations} />
      </PageTitleRow>
      <div className="flex min-h-0 flex-1 flex-col px-4 pt-2 pb-4">
        <FullCalendarPanel
          events={eventsData?.events ?? []}
          initialView={(init.view as CalendarViewKey) ?? "dayGridMonth"}
          initialDate={normalizeInitialDate(init.date, init.view)}
          gridSize={filter?.gridSize ?? 30}
          firstHour={firstHour}
          alternateHoursBackground={
            eventsData?.alternateHoursBackground ??
            init.alternateHoursBackground ??
            false
          }
          onRangeChange={onRangeChange}
          apiRef={apiRef}
        />
      </div>
    </>
  );
}
