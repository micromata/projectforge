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
import { MenuLink } from "@/components/shared/menu-link";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import type { VacationAccount } from "./types";

/**
 * The corrections of the leave account of the previous and current year, newest first. A row opens its form, which
 * returns here, as does a new entry (HR staff only). While another employee's account loads (`switching`), the new
 * entry is disabled: it would be of the one still shown.
 */
export function LeaveAccountEntriesTable({
  account,
  switching,
}: {
  account: VacationAccount;
  switching: boolean;
}) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();
  const addLabel = (
    <>
      <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
      {t("vacation.leaveAccountEntry.title.add")}
    </>
  );

  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">
          {t("vacation.leaveAccountEntry.title.heading")}
        </h2>
        {account.newLeaveAccountEntryUrl &&
          (switching ? (
            <Button type="button" size="sm" className="gap-1.5" disabled>
              {addLabel}
            </Button>
          ) : (
            <Button asChild size="sm" className="gap-1.5">
              <MenuLink url={account.newLeaveAccountEntryUrl}>
                {addLabel}
              </MenuLink>
            </Button>
          ))}
      </div>
      <div className="overflow-x-auto rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("date._")}</TableHead>
              <TableHead className="text-right">
                {t("vacation.leaveAccountEntry.amount")}
              </TableHead>
              <TableHead>{t("description")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {account.leaveAccountEntries.map((entry) => (
              <TableRow
                key={entry.id}
                className="cursor-pointer"
                onClick={() => navigate(entry.editUrl)}
              >
                <TableCell className="whitespace-nowrap">
                  <MenuLink
                    url={entry.editUrl}
                    className="text-primary hover:underline"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {entry.dateFormatted}
                  </MenuLink>
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {entry.amountFormatted}
                </TableCell>
                <TableCell className="whitespace-normal">
                  {entry.description}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </section>
  );
}
