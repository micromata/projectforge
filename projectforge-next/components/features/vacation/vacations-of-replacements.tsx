"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  SubstituteAbsencesTable,
  type SubstituteAbsenceRow,
} from "@/components/shared/absence-chart/substitute-absences-table";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import { useVacationStats } from "./use-vacation-stats";

/**
 * The vacations of the replacements overlapping this one — the React form's grid below the fields, which
 * tells why there is a conflict and whom to pick instead. Dates and days as the backend formatted them. Then their
 * availability entries: what they're called (the type only for the replacement and HR staff) in the place of the
 * status — a not-available one counts as a vacation, the others are told only.
 */
export function VacationsOfReplacements() {
  const t = useTranslations();
  const label = useFieldLabels(VACATION_METADATA);
  const stats = useVacationStats();
  const availabilityTitle = t("availability.title._");
  const rows = useMemo<SubstituteAbsenceRow[]>(() => {
    const vacations = stats?.conflictingVacations ?? [];
    return [
      ...vacations.map((v, index) => ({
        id: String(v.id ?? index),
        name: v.employee,
        startDateFormatted: v.startDateFormatted,
        endDateFormatted: v.endDateFormatted,
        kind: v.vacationModeString,
        status: v.statusString,
        extra: {
          replacement: v.replacement,
          otherReplacements: v.otherReplacements,
          manager: v.manager,
          workingDays: v.workingDaysFormatted,
        },
      })),
      ...(stats?.replacementAvailabilities ?? []).map((entry, i) => ({
        id: `a${i}`,
        name: entry.employee ?? entry.user,
        startDateFormatted: entry.startDateFormatted,
        endDateFormatted: entry.endDateFormatted,
        kind: availabilityTitle,
        status: entry.label,
      })),
    ];
  }, [
    stats?.conflictingVacations,
    stats?.replacementAvailabilities,
    availabilityTitle,
  ]);
  return (
    <SubstituteAbsencesTable
      rows={rows}
      headers={{
        name: label("employee"),
        startDate: label("startDate"),
        endDate: label("endDate"),
        kind: t("vacation.vacationmode"),
        status: label("status"),
      }}
      extraColumns={[
        { id: "replacement", header: label("replacement") },
        { id: "otherReplacements", header: t("vacation.replacement.others") },
        { id: "manager", header: label("manager") },
        { id: "workingDays", header: t("vacation.workingdays"), size: 90 },
      ]}
      unavailableText={
        stats?.unavailable
          ? t("vacation.replacementVacationsUnavailable")
          : null
      }
    />
  );
}
