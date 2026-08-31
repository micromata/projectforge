"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { CalendarOptions } from "@fullcalendar/core";
import { useFormatContext } from "@/hooks/use-format";
import { buildViews, HEADER_TOOLBAR } from "./view-config";

/**
 * The header's data- and locale-dependent pieces: the `views` map (with the slot size / scroll
 * position from the filter and the localised button labels) and the localised "today" label. The
 * static button layout stays in `view-config.ts` (`HEADER_TOOLBAR`); the "new entry" button is the
 * page-level AddEntryButton, not a FullCalendar custom button (see CalendarPage).
 */
export function useViewButtons({
  gridSize,
  firstHour,
}: {
  gridSize: number;
  firstHour: number;
}) {
  const t = useTranslations("calendar");
  const { hour12 } = useFormatContext();

  const views = useMemo(
    () =>
      buildViews({
        gridSize,
        firstHour,
        hour12,
        labels: {
          // `calendar.month` is also a namespace (the recurrence editor's month names), so its own
          // value lives under `_` (see i18n/config.ts).
          month: t("month._"),
          week: t("week"),
          day: t("day"),
          workDays: t("view.workDays"),
          overview: t("view.overview"),
          agenda: t("view.agenda"),
        },
      }),
    [gridSize, firstHour, hour12, t]
  );

  const buttonText: CalendarOptions["buttonText"] = useMemo(
    () => ({ today: t("today") }),
    [t]
  );

  return { views, headerToolbar: HEADER_TOOLBAR, buttonText };
}
