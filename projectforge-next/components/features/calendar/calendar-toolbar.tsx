"use client";

import type { CalendarInit } from "@/lib/rs/calendar-types";
import type { useCalendarFilterMutations } from "./use-calendar-filter-mutations";
import { CalendarFavoritesMenu } from "./calendar-favorites-menu";
import { CalendarSettingsDialog } from "./calendar-settings-dialog";
import { CalendarMoreMenu } from "./calendar-more-menu";

/**
 * The calendar's own header controls: the saved-filter favourites, the settings gear and the overflow
 * menu. The calendar chooser sits on its own row below the title (see CalendarPage), so this stays a
 * compact right-aligned group. The filter mutations are created once on the page and shared here.
 */
export function CalendarToolbar({
  init,
  mutations,
}: {
  init: CalendarInit;
  mutations: ReturnType<typeof useCalendarFilterMutations>;
}) {
  return (
    <div className="flex items-center gap-1">
      <CalendarFavoritesMenu
        favorites={init.filterFavorites ?? []}
        currentFilterId={init.filter?.id}
        isFilterModified={init.isFilterModified}
      />
      <CalendarSettingsDialog init={init} mutations={mutations} />
      <CalendarMoreMenu onRefresh={mutations.refresh} />
    </div>
  );
}
