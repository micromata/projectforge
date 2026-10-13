"use client";

import { useTranslations } from "next-intl";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { VacationAccount, VacationStatsFormatted } from "./types";

interface StatsRow {
  label: string;
  field: keyof Omit<VacationStatsFormatted, "year">;
  /** A sum: what the rows above it add up to. */
  sum?: boolean;
}

/** Whether anything is booked in the year yet: the next year is shown only then. */
function hasBookings(stats: VacationStatsFormatted): boolean {
  return [
    stats.vacationDaysApproved,
    stats.vacationDaysInProgress,
    stats.specialVacationDaysApproved,
    stats.specialVacationDaysInProgress,
  ].some(Boolean);
}

/**
 * The leave account as the React page's VacationStatistics showed it: the current year before the previous one,
 * the rows from the annual leave down to the special leave, a row left out when it has no value in any year, the
 * sums bold between rules. The next year joins in front once a vacation is booked in it (the React page computed
 * it, but never showed it).
 */
export function VacationStatisticsTable({
  account,
}: {
  account: VacationAccount;
}) {
  const t = useTranslations();
  // The keys spelled out: the i18n scan finds literal keys only.
  const rows: StatsRow[] = [
    {
      label: t("vacation.annualleave"),
      field: "vacationDaysInYearFromContract",
    },
    {
      label: t("vacation.previousyearleave"),
      field: "remainingLeaveFromPreviousYear",
    },
    {
      label: t("vacation.subtotal"),
      field: "totalLeaveIncludingCarry",
      sum: true,
    },
    {
      label: account.previousYearLeaveUnusedLabel ?? "",
      field: "remainingLeaveFromPreviousYearUnused",
    },
    {
      label: t("menu.vacation.leaveAccountEntry"),
      field: "leaveAccountEntriesSum",
    },
    { label: t("vacation.vacationApproved"), field: "vacationDaysApproved" },
    {
      label: t("vacation.vacationInProgress"),
      field: "vacationDaysInProgress",
    },
    {
      label: t("vacation.availablevacation"),
      field: "vacationDaysLeftInYear",
      sum: true,
    },
    {
      label: t("vacation.specialApproved"),
      field: "specialVacationDaysApproved",
    },
    {
      label: t("vacation.specialInProgress"),
      field: "specialVacationDaysInProgress",
    },
  ];

  const [previous, current, next] = account.statistics;
  const years = [
    next && hasBookings(next) ? next : null,
    current,
    previous,
  ].filter((stats): stats is VacationStatsFormatted => !!stats);

  return (
    <div className="max-w-full overflow-x-auto">
      <Table className="w-auto">
        <TableHeader>
          <TableRow className="border-b border-foreground hover:bg-transparent">
            <TableHead className="h-7 font-semibold">
              {t("vacation.leaveaccount.title")}
            </TableHead>
            {years.map((stats) => (
              <TableHead
                key={stats.year}
                className="h-7 pl-6 text-right font-semibold"
              >
                {stats.year}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows
            .filter((row) => years.some((stats) => stats[row.field]))
            .map((row) => (
              <TableRow
                key={row.field}
                className={cn(
                  "even:bg-muted/40",
                  row.sum && "border-t border-t-foreground/40 font-semibold"
                )}
              >
                <TableCell className="py-1">{row.label}</TableCell>
                {years.map((stats) => (
                  <TableCell
                    key={stats.year}
                    className="py-1 pl-6 text-right whitespace-nowrap tabular-nums"
                  >
                    {stats[row.field]}
                  </TableCell>
                ))}
              </TableRow>
            ))}
        </TableBody>
      </Table>
    </div>
  );
}
