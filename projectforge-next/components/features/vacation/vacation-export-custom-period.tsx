"use client";

import { useTranslations } from "next-intl";
import { DateInput } from "@/components/shared/date-input";
import { MAX_SPAN_DAYS } from "./vacation-export-period";

/**
 * The two days of a free range (`von – bis`) of the team vacation calendar, beside the stepper once "Eigener
 * Zeitraum" is chosen. A day cleared is not applied — there is no range without both. Said while it applies
 * that the end was cut to what may be shown at once ([MAX_SPAN_DAYS]).
 */
export function VacationExportCustomPeriod({
  from,
  until,
  shortened,
  onChange,
}: {
  from: string;
  until: string;
  shortened: boolean;
  onChange: (from: string, until: string) => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-1">
        <DateInput
          className="w-40"
          aria-label={t("date.from")}
          value={from}
          onChange={(next) => next && onChange(next, until)}
        />
        <span aria-hidden className="text-muted-foreground">
          –
        </span>
        <DateInput
          className="w-40"
          aria-label={t("until")}
          value={until}
          defaultMonth={from}
          onChange={(next) => next && onChange(from, next)}
        />
      </div>
      {shortened && (
        <p role="status" className="text-xs text-muted-foreground">
          {t("vacation.export.period.maxDays", { arg0: MAX_SPAN_DAYS })}
        </p>
      )}
    </div>
  );
}
