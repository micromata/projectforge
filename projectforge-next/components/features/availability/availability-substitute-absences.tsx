"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  SubstituteAbsencesTable,
  type SubstituteAbsenceRow,
} from "@/components/shared/absence-chart/substitute-absences-table";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { AVAILABILITY_METADATA } from "@/lib/metadata/availability.generated";
import { useAvailabilityStats } from "./use-availability-stats";

/**
 * The absences of the substitutes overlapping the entry, with all their details (the chart's text alternative): their
 * vacations with status and working days, then their availability entries, called as the user may see them (the type
 * only for the substitute and HR staff) in the place of the status.
 */
export function AvailabilitySubstituteAbsences() {
  const t = useTranslations();
  const label = useFieldLabels(AVAILABILITY_METADATA);
  const stats = useAvailabilityStats();
  const vacationTitle = t("vacation._");
  const availabilityTitle = t("availability.title._");
  const rows = useMemo<SubstituteAbsenceRow[]>(
    () => [
      ...(stats?.substituteVacations ?? []).map((v, index) => ({
        id: String(v.id ?? `v${index}`),
        name: v.employee?.user?.displayName ?? v.employee?.displayName,
        startDateFormatted: v.startDateFormatted,
        endDateFormatted: v.endDateFormatted,
        kind: vacationTitle,
        status: v.statusString,
        extra: { workingDays: v.workingDaysFormatted },
      })),
      ...(stats?.substituteAvailabilities ?? []).map((entry, i) => ({
        id: `a${i}`,
        name: entry.user?.displayName ?? entry.employee?.displayName,
        startDateFormatted: entry.startDateFormatted,
        endDateFormatted: entry.endDateFormatted,
        kind: availabilityTitle,
        status: entry.label,
      })),
    ],
    [
      stats?.substituteVacations,
      stats?.substituteAvailabilities,
      vacationTitle,
      availabilityTitle,
    ]
  );
  return (
    <SubstituteAbsencesTable
      rows={rows}
      headers={{
        name: t("availability.substitutes"),
        startDate: label("startDate"),
        endDate: label("endDate"),
        kind: t("availability.substitutes.kind"),
        status: label("status"),
      }}
      extraColumns={[
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
