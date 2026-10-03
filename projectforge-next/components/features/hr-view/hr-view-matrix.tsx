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
import { cn } from "@/lib/utils";
import { HrViewCell } from "./hr-view-cell";
import { bookedHref, planningHref } from "./planning-links";
import type { HrView, HrViewRow } from "./types";

/**
 * The matrix of the HR view: one row per employee, the sum and the rest (planned without a project), then
 * one column per project or customer. A cell reads "planned (booked)" in days of 8 hours.
 *
 * The employee leads to the planned week (only with write access), a deleted planning struck through; the
 * booked days of the sum and of a project lead to the time sheets of the employee in the period.
 */
export function HrViewMatrix({ view }: { view: HrView }) {
  const t = useTranslations();

  function userCell(row: HrViewRow) {
    const name = row.userName ?? String(row.userId);
    const url = planningHref(view, row.userId, row.planningId);
    const className = cn(row.deleted && "line-through");
    return url ? (
      <MenuLink
        url={url}
        className={cn("text-primary hover:underline", className)}
      >
        {name}
      </MenuLink>
    ) : (
      <span className={className}>{name}</span>
    );
  }

  return (
    <div className="overflow-x-auto rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("timesheet.user")}</TableHead>
            <TableHead className="text-right">{t("sum")}</TableHead>
            <TableHead className="text-right">{t("rest")}</TableHead>
            {view.columns.map((column) => (
              <TableHead key={column.key} className="text-right">
                {column.label}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {view.rows.map((row) => (
            <TableRow key={row.userId}>
              <TableCell className="whitespace-nowrap">
                {userCell(row)}
              </TableCell>
              <HrViewCell
                cell={row.sum}
                bookedUrl={bookedHref(view, row)}
                className="text-right font-medium tabular-nums"
              />
              <HrViewCell cell={row.rest} />
              {view.columns.map((column) => (
                <HrViewCell
                  key={column.key}
                  cell={row.cells[column.key]}
                  bookedUrl={
                    column.taskId
                      ? bookedHref(view, row, column.taskId, column.label)
                      : undefined
                  }
                />
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
