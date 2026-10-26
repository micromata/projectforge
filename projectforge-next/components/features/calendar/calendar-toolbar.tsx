"use client";

import { useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { BookDaysButton } from "@/components/features/timesheet/book-days/book-days-button";
import { AvailabilityQuickViewButton } from "@/components/shared/availability-quick-view/availability-quick-view-button";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import type { CalendarInit } from "@/lib/rs/calendar-types";
import type { useCalendarFilterMutations } from "./use-calendar-filter-mutations";
import { CalendarFavoritesMenu } from "./calendar-favorites-menu";
import { CalendarSettingsDialog } from "./calendar-settings-dialog";
import { CalendarMoreMenu } from "./calendar-more-menu";
import { CALENDAR_EVENTS_KEY } from "./use-calendar-init";
import { toPickerRef } from "@/lib/page-def/reference";

/**
 * The calendar's own header controls: booking several days at once, the saved-filter favourites, the
 * settings gear and the overflow menu. The calendar chooser sits on its own row below the title (see CalendarPage), so this stays a
 * compact right-aligned group. The filter mutations are created once on the page and shared here.
 */
export function CalendarToolbar({
  init,
  mutations,
  getViewStart,
  showDate,
}: {
  init: CalendarInit;
  mutations: ReturnType<typeof useCalendarFilterMutations>;
  /** First day of the period the calendar currently shows, as ISO date. */
  getViewStart: () => string | null;
  /** Moves the calendar to the period of an ISO date, keeping the view. */
  showDate: (iso: string) => void;
}) {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUserRef();
  const router = useRouter();
  // `?bookDays=1` (e.g. from the vacation days without time sheets of the monthly report) opens the
  // dialog once; the parameter is dropped then, so a reload doesn't open it again.
  const openBookDays = useSearchParams().get("bookDays") != null;
  const onBookDaysOpened = useCallback(
    () => router.replace("/calendar"),
    [router]
  );
  const canChooseUser = init.filter?.otherTimesheetUsersEnabled === true;
  // The user whose sheets the calendar shows — a positive id is a real user only for those who may see
  // others (everyone else gets the 1/-1 on/off sentinel, see CalendarSettingsDialog).
  const filterUser = init.timesheetUser;
  const shownUser =
    canChooseUser &&
    (init.filter?.timesheetUserId ?? 0) > 0 &&
    filterUser?.id != null
      ? toPickerRef({ ...filterUser, id: filterUser.id })
      : null;
  // Redraw the booked days, moved to the period they were booked in. Booked for oneself while the calendar hides time sheets (the default for
  // a new colleague), the own sheets are turned on so the booking is visible at all — as a sheet saved
  // in the calendar does (see activateOwnTimesheets).
  const onBooked = (userId: number | null, firstDay: string | null) => {
    if (firstDay) showDate(firstDay);
    void queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY });
    const ownId = currentUser?.id;
    if (ownId != null && (userId ?? ownId) === ownId) {
      if ((init.timesheetUser?.id ?? 0) <= 0) {
        void mutations.changeTimesheetUser(ownId);
      }
    }
  };

  return (
    <div className="flex items-center gap-1">
      <BookDaysButton
        defaultUser={shownUser ?? currentUser}
        canChooseUser={canChooseUser}
        getViewStart={getViewStart}
        onBooked={onBooked}
        openOnMount={openBookDays}
        onOpened={onBookDaysOpened}
      />
      <AvailabilityQuickViewButton iconOnly />
      <CalendarFavoritesMenu
        favorites={init.filterFavorites ?? []}
        currentFilterId={init.filter?.id}
        isFilterModified={init.isFilterModified}
      />
      <CalendarSettingsDialog init={init} mutations={mutations} />
      <CalendarMoreMenu
        onRefresh={mutations.refresh}
        defaultCalendarId={init.filter?.defaultCalendarId}
      />
    </div>
  );
}
