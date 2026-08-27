"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { CalendarOptions } from "@fullcalendar/core";
import { useFormatContext } from "@/hooks/use-format";
import { buildViews, HEADER_TOOLBAR } from "./view-config";

/**
 * The header's data- and locale-dependent pieces: the `views` map (with the slot size / scroll
 * position from the filter and the localised button labels), the "+" create button, and the localised
 * "today" label. The static button layout stays in `view-config.ts` (`HEADER_TOOLBAR`).
 */
export function useViewButtons({
  gridSize,
  firstHour,
  onCreate,
}: {
  gridSize: number;
  firstHour: number;
  onCreate: () => void;
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

  const customButtons: CalendarOptions["customButtons"] = useMemo(
    () => ({
      addEvent: { text: "+", hint: t("newEntry"), click: onCreate },
    }),
    [t, onCreate]
  );

  const buttonText: CalendarOptions["buttonText"] = useMemo(
    () => ({ today: t("today") }),
    [t]
  );

  return { views, headerToolbar: HEADER_TOOLBAR, customButtons, buttonText };
}
