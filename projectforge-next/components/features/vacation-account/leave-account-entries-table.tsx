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
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import type { VacationAccount } from "./types";

/**
 * The corrections of the leave account of the previous and current year, newest first. A row opens its form
 * (still the legacy React page), which returns here.
 */
export function LeaveAccountEntriesTable({
  account,
}: {
  account: VacationAccount;
}) {
  const t = useTranslations();
  const navigate = useNavigateMenuUrl();

  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-sm font-semibold">
        {t("vacation.leaveAccountEntry.title.heading")}
      </h2>
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
