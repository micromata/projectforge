"use client";

import { useTranslations } from "next-intl";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { cn } from "@/lib/utils";
import { useVacationStats } from "./use-vacation-stats";

/** A value the backend computes for the form, shown like a field that can't be changed. */
function StatsValue({
  label,
  value,
  className,
  valueClassName,
}: {
  label: string;
  value: string | null | undefined;
  className?: string;
  valueClassName?: string;
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
        className={cn(
          "flex h-7 min-w-0 items-center rounded-md border border-input bg-input/20 px-2 text-xs/relaxed text-muted-foreground tabular-nums dark:bg-input/30",
          valueClassName
        )}
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

/**
 * The days left in the year of the start, of the entry's employee (`VacationService.getVacationStats`), and
 * what is left once this entry is counted as it is being edited — where that differs, as for a new entry or a
 * changed period. Red where it is not enough: the save is refused then (`VacationValidator`).
 */
export function VacationDaysLeftField({ className }: { className?: string }) {
  const t = useTranslations();
  const stats = useVacationStats();
  const left = stats?.vacationDaysLeftInYearString;
  const after = stats?.vacationDaysLeftAfterThisString;
  const changed =
    after != null &&
    stats?.vacationDaysLeftAfterThis !== stats?.vacationDaysLeftInYear;
  return (
    <StatsValue
      label={t("vacation.availabledays")}
      value={
        changed
          ? t("vacation.availabledaysAfterThis", {
              arg0: left ?? "",
              arg1: after,
            })
          : left
      }
      valueClassName={cn(
        changed &&
          (stats?.vacationDaysLeftAfterThis ?? 0) < 0 &&
          "text-destructive"
      )}
      className={className}
    />
  );
}
