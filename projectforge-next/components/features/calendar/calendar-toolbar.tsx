"use client";

import type { CalendarInit } from "@/lib/rs/calendar-types";
import { useCalendarFilterMutations } from "./use-calendar-filter-mutations";
import { CalendarSelect } from "./calendar-select";
import { CalendarFavoritesMenu } from "./calendar-favorites-menu";
import { CalendarSettingsDialog } from "./calendar-settings-dialog";
import { CalendarMoreMenu } from "./calendar-more-menu";

/**
 * The calendar's own controls, above the grid: the calendar chooser, the saved-filter favourites, the
 * settings gear and the overflow menu. It wraps on a narrow screen rather than overflowing (the pills
 * flow, the menus stay to the right). The filter mutations are created once here and handed to the
 * pieces that trigger them.
 */
export function CalendarToolbar({ init }: { init: CalendarInit }) {
  const mutations = useCalendarFilterMutations();

  return (
    <div className="flex flex-wrap items-center gap-2">
      <CalendarSelect
        teamCalendars={init.teamCalendars ?? []}
        activeCalendars={init.activeCalendars ?? []}
        onSetActive={mutations.setActiveCalendars}
        onSetVisibility={mutations.setVisibility}
        onChangeStyle={mutations.changeStyle}
      />
      <div className="ml-auto flex items-center gap-1">
        <CalendarFavoritesMenu
          favorites={init.filterFavorites ?? []}
          currentFilterId={init.filter?.id}
          isFilterModified={init.isFilterModified}
        />
        <CalendarSettingsDialog init={init} mutations={mutations} />
        <CalendarMoreMenu onRefresh={mutations.refresh} />
      </div>
    </div>
  );
}
