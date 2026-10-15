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
import { MenuLink } from "@/components/shared/menu-link";
import { VacationDaysBar } from "@/components/features/vacation/vacation-days-bar";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { cn } from "@/lib/utils";
import type { VacationAccount } from "./types";

/**
 * The vacations of the next, current and previous year, one block per year. A row opens its form, which returns
 * here; an entry with a day without any replacement on duty is red (the React page's `ag-row-red`, styled
 * nowhere there).
 */
export function VacationEntriesTable({
  account,
}: {
  account: VacationAccount;
}) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();
  const columns = 10;

  return (
    <div className="overflow-x-auto rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("vacation.startdate")}</TableHead>
            <TableHead>{t("vacation.enddate")}</TableHead>
            <TableHead>{t("vacation.status._")}</TableHead>
            <TableHead>{t("vacation.Days")}</TableHead>
            <TableHead>{t("vacation.special._")}</TableHead>
            <TableHead>{t("vacation.replacement._")}</TableHead>
            <TableHead>{t("vacation.replacement.others")}</TableHead>
            <TableHead>{t("vacation.manager")}</TableHead>
            <TableHead>{t("vacation.vacationmode")}</TableHead>
            <TableHead>{t("comment")}</TableHead>
          </TableRow>
        </TableHeader>
        {account.years.map(({ year, vacations }) => (
          <TableBody key={year} className="border-t-2">
            <TableRow className="hover:bg-transparent">
              <TableCell colSpan={columns} className="font-semibold">
                {year}
              </TableCell>
            </TableRow>
            {vacations.map((vacation) => (
              <TableRow
                key={vacation.id}
                className={cn("cursor-pointer", vacation.conflict && "row-red")}
                onClick={() => navigate(vacation.editUrl)}
              >
                <TableCell className="whitespace-nowrap">
                  {/* The link makes the row reachable by keyboard; the row click is the mouse's shortcut. */}
                  <MenuLink
                    url={vacation.editUrl}
                    className="text-primary hover:underline"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {vacation.startDateFormatted}
                  </MenuLink>
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  {vacation.endDateFormatted}
                </TableCell>
                <TableCell>{vacation.statusString}</TableCell>
                <TableCell>
                  {/* As in the vacation list: beside the number the period as a bar (VacationDaysBar). */}
                  <div className="flex items-center gap-3">
                    <span className="w-8 shrink-0 text-right tabular-nums">
                      {vacation.workingDaysFormatted}
                    </span>
                    <VacationDaysBar row={vacation} />
                  </div>
                </TableCell>
                <TableCell>{vacation.specialFormatted}</TableCell>
                <TableCell>{vacation.replacement}</TableCell>
                <TableCell className="whitespace-normal">
                  {vacation.otherReplacements}
                </TableCell>
                <TableCell>{vacation.manager}</TableCell>
                <TableCell>{vacation.vacationModeString}</TableCell>
                <TableCell className="whitespace-normal">
                  {vacation.comment}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        ))}
      </Table>
    </div>
  );
}
