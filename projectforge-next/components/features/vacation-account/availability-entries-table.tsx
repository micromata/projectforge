"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { AbsenceDaysCell } from "@/components/shared/absence-chart/absence-days-bar";
import { MenuLink } from "@/components/shared/menu-link";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { VacationAccount } from "./types";
import { RefList } from "@/components/shared/ref-list";

/**
 * The availability entries of the employee's user overlapping the three years of the account, newest first: the
 * other absences, apart from the vacations. A row opens its form, which returns here, as does a new entry; an entry
 * with a day without any substitute on duty is red, as in the availability list. While another employee's account
 * loads (`switching`), the new entry is disabled: it would be of the one still shown.
 */
export function AvailabilityEntriesTable({
  account,
  switching,
}: {
  account: VacationAccount;
  switching: boolean;
}) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();
  const columns = 8;
  const addLabel = (
    <>
      <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
      {t("availability.entry.add")}
    </>
  );

  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">
          {t("vacation.leaveaccount.availabilities")}
        </h2>
        {account.newAvailabilityUrl &&
          (switching ? (
            <Button type="button" size="sm" className="gap-1.5" disabled>
              {addLabel}
            </Button>
          ) : (
            <Button asChild size="sm" className="gap-1.5">
              <MenuLink url={account.newAvailabilityUrl}>{addLabel}</MenuLink>
            </Button>
          ))}
      </div>
      <div className="overflow-x-auto rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("availability.startDate")}</TableHead>
              <TableHead>{t("availability.endDate")}</TableHead>
              <TableHead>{t("availability.workingDays")}</TableHead>
              <TableHead>{t(leafKeyOf("availability.type", t.has))}</TableHead>
              <TableHead>
                {t(leafKeyOf("availability.status", t.has))}
              </TableHead>
              <TableHead>
                {t(leafKeyOf("availability.location", t.has))}
              </TableHead>
              <TableHead>
                {t(leafKeyOf("availability.comment", t.has))}
              </TableHead>
              <TableHead>
                {t(leafKeyOf("availability.substitutes", t.has))}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {account.availabilities.length === 0 && (
              <TableRow className="hover:bg-transparent">
                <TableCell colSpan={columns} className="text-muted-foreground">
                  {t("nothingFound")}
                </TableCell>
              </TableRow>
            )}
            {account.availabilities.map((entry) => (
              <TableRow
                key={entry.id}
                className={cn("cursor-pointer", entry.conflict && "row-red")}
                onClick={() => navigate(entry.editUrl)}
              >
                <TableCell className="whitespace-nowrap">
                  {/* The link makes the row reachable by keyboard; the row click is the mouse's shortcut. */}
                  <MenuLink
                    url={entry.editUrl}
                    className="text-primary hover:underline"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {entry.startDateFormatted}
                  </MenuLink>
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  {entry.endDateFormatted}
                </TableCell>
                <TableCell>
                  {/* As in the availability list: beside the number the period as a bar. */}
                  <AbsenceDaysCell
                    formatted={entry.workingDaysFormatted}
                    row={entry}
                  />
                </TableCell>
                <TableCell>{entry.type}</TableCell>
                <TableCell>{entry.statusString}</TableCell>
                <TableCell>{entry.locationString}</TableCell>
                <TableCell className="whitespace-normal">
                  {entry.comment}
                </TableCell>
                <TableCell className="whitespace-normal">
                  <RefList refs={entry.substitutes} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </section>
  );
}
