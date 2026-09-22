"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import { useFormatContext } from "@/hooks/use-format";
import { formatMonthName } from "@/lib/format";
import { periodKindOf } from "@/lib/date-period";
import { isoOfParts, partsOf } from "@/lib/date-period-math";
import type { MonthlyReport, MonthlyReportQuery } from "./types";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

// The report's month is a whole calendar month, the same art the list filters page — so the shared
// PeriodStepper drives it (◀ previous / current 📅 / next ▶, Wicket's QuickSelectMonthPanel). Its
// `currentButton` mode drops the art dropdown, redundant here next to the explicit month select.
const MONTH_KIND = periodKindOf("month")!;

/**
 * The report filter: the user (only when the account may read other users' time sheets), the year and the
 * month. The year list is the years that have time sheets for the user; the selects show the resolved
 * year/month of the report, so they stay in sync with what is displayed even when the query defaulted them.
 */
export function ReportFilterRow({
  report,
  value,
  onChange,
}: {
  report?: MonthlyReport;
  value: MonthlyReportQuery;
  onChange: (query: MonthlyReportQuery) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const currentUser = useCurrentUserRef();
  if (!report) return null;

  // The report's own available years, current year always among them so a fresh month is selectable.
  const years = Array.from(
    new Set([report.year, ...report.availableYears])
  ).sort((a, b) => b - a);

  return (
    <div className="flex flex-wrap items-end gap-4">
      {report.maySelectOtherUsers && (
        <div className="flex flex-col gap-1">
          <Label htmlFor="report-user">{t("timesheet.user")}</Label>
          <EntityAutocomplete
            id="report-user"
            url="user/autosearch?search=:search"
            value={
              report.userId
                ? { id: report.userId, displayName: report.userName }
                : null
            }
            selectMe={currentUser}
            aria-label={t("timesheet.user")}
            onChange={(user) => onChange({ ...value, userId: user?.id })}
          />
        </div>
      )}
      <div className="flex flex-col gap-1">
        <Label htmlFor="report-year">{t("calendar.year")}</Label>
        <Select
          value={String(report.year)}
          onValueChange={(year) => onChange({ ...value, year: Number(year) })}
        >
          <SelectTrigger id="report-year" className="h-9 w-28">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {years.map((year) => (
              <SelectItem key={year} value={String(year)}>
                {year}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="report-month">{t("calendar.month._")}</Label>
        <Select
          value={String(report.month)}
          onValueChange={(month) =>
            onChange({ ...value, month: Number(month) })
          }
        >
          <SelectTrigger id="report-month" className="h-9 w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {MONTHS.map((month) => (
              <SelectItem key={month} value={String(month)}>
                {formatMonthName(month, ctx)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <PeriodStepper
        className="pb-0.5"
        currentButton
        kinds={[MONTH_KIND]}
        current={{
          kind: MONTH_KIND,
          anchor: isoOfParts(report.year, report.month, 1),
        }}
        onSelect={(_, anchor) => {
          const { year, month } = partsOf(anchor);
          onChange({ ...value, year, month });
        }}
      />
    </div>
  );
}
