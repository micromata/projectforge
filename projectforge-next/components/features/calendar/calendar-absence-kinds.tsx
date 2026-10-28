"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { AbsenceKindsSelect } from "@/components/shared/absence-kinds/absence-kinds-select";
import { ABSENCE_KINDS, type AbsenceKind } from "@/lib/rs/calendar-types";

interface CalendarAbsenceKindsProps {
  /** The stored choice; null for all kinds. */
  kinds: AbsenceKind[] | null | undefined;
  onChange: (kinds: AbsenceKind[]) => void;
}

/**
 * What the calendar shows of the absences of the vacation groups and users (`changeAbsenceKinds`), not of the team
 * calendars (they show what they include). The picks are held here, as the vacation pickers do, because the server
 * answers a change with `isFilterModified` only.
 */
export function CalendarAbsenceKinds({
  kinds,
  onChange,
}: CalendarAbsenceKindsProps) {
  const t = useTranslations();
  const [value, setValue] = useState<AbsenceKind[]>(() => [
    ...(kinds ?? ABSENCE_KINDS),
  ]);
  return (
    <AbsenceKindsSelect
      value={value}
      onChange={(next) => {
        setValue(next);
        onChange(next);
      }}
      label={t("calendar.filter.absenceKinds._")}
      hint={t("calendar.filter.absenceKinds.tooltip")}
    />
  );
}
