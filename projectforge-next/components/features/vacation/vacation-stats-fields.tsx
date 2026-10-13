"use client";

import { useTranslations } from "next-intl";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useVacationStats } from "./use-vacation-stats";

/** A value the backend computes for the form, shown like a field that can't be changed. */
function StatsValue({
  label,
  value,
  className,
}: {
  label: string;
  value: string | null | undefined;
  className?: string;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      label={label}
      readOnly
      invalid={false}
      errors={[]}
      className={className}
      ids={ids}
    >
      <div
        id={ids.controlId}
        aria-labelledby={ids.labelId}
        className="flex h-7 min-w-0 items-center rounded-md border border-input bg-input/20 px-2 text-xs/relaxed text-muted-foreground tabular-nums dark:bg-input/30"
      >
        {value ?? ""}
      </div>
    </FieldShell>
  );
}

/** The working days of the period, half days counted as such (`VacationService.getVacationDays`). */
export function VacationWorkingDaysField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const stats = useVacationStats();
  return (
    <StatsValue
      label={t("vacation.workingdays")}
      value={stats?.workingDaysFormatted}
      className={className}
    />
  );
}

/** The days left in the year of the start, of the entry's employee (`VacationService.getVacationStats`). */
export function VacationDaysLeftField({ className }: { className?: string }) {
  const t = useTranslations();
  const stats = useVacationStats();
  return (
    <StatsValue
      label={t("vacation.availabledays")}
      value={stats?.vacationDaysLeftInYearString}
      className={className}
    />
  );
}
