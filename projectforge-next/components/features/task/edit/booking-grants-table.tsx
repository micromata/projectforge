"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type {
  TaskBookingGrant,
  TaskBookingGrantStatus,
} from "@/lib/rs/task-booking-grant";
import { cn } from "@/lib/utils";

/** The grants that still count: they may be revoked. */
const REVOCABLE: ReadonlySet<TaskBookingGrantStatus> = new Set([
  "ACTIVE",
  "UPCOMING",
  "PENDING",
]);

/**
 * The ad hoc booking grants of a task, newest period first, as the backend sorted them. Revoked, expired and
 * lapsed ones are muted and kept: they say who booked on the task by a grant.
 */
export function BookingGrantsTable({
  grants,
  onRevoke,
}: {
  grants: TaskBookingGrant[];
  onRevoke: (grant: TaskBookingGrant) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <div className="overflow-x-auto rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("user._")}</TableHead>
            <TableHead>{t("task.bookingGrant.validFrom")}</TableHead>
            <TableHead>{t("task.bookingGrant.validUntil")}</TableHead>
            <TableHead>{t("fibu.kost2._")}</TableHead>
            <TableHead>{t("task.bookingGrant.status._")}</TableHead>
            <TableHead>{t("task.bookingGrant.grantedBy")}</TableHead>
            <TableHead>{t("comment")}</TableHead>
            <TableHead>
              <span className="sr-only">{t("task.bookingGrant.revoke._")}</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {grants.map((grant) => {
            const revocable = REVOCABLE.has(grant.status);
            return (
              <TableRow
                key={grant.id}
                className={cn(!revocable && "text-muted-foreground")}
              >
                <TableCell>{grant.userName}</TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatDate(grant.validFrom, ctx)}
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatDate(grant.validUntil, ctx)}
                </TableCell>
                <TableCell>
                  {grant.kost2Title ?? t("task.bookingGrant.kost2.all")}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={
                      grant.status === "ACTIVE" ? "default" : "secondary"
                    }
                  >
                    {grant.statusAsString}
                  </Badge>
                </TableCell>
                <TableCell>{grant.grantedByName}</TableCell>
                <TableCell className="whitespace-normal">
                  {grant.comment}
                </TableCell>
                <TableCell className="text-right">
                  {revocable && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      aria-label={`${t("task.bookingGrant.revoke._")}: ${grant.userName ?? ""}`}
                      onClick={() => onRevoke(grant)}
                    >
                      {t("task.bookingGrant.revoke._")}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
