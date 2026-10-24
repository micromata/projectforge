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
import type { TaskBookingGrant } from "@/lib/rs/task-booking-grant";
import { cn } from "@/lib/utils";
import { BookingGrantPositionList } from "./booking-grant-position-list";

/**
 * The ad hoc booking grants of a task and its sub elements, newest period first, as the backend sorted them.
 * Revoked and expired ones are muted and kept: they say who booked on the tasks by a grant. Whether a grant
 * may be revoked is the backend's to say (`revocable`: current, and grantable on all of its tasks).
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
            <TableHead>{t("task.bookingGrant.positions._")}</TableHead>
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
            const current =
              grant.status === "ACTIVE" || grant.status === "UPCOMING";
            return (
              <TableRow
                key={grant.id}
                className={cn(!current && "text-muted-foreground")}
              >
                <TableCell>{grant.userName}</TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatDate(grant.validFrom, ctx)}
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatDate(grant.validUntil, ctx)}
                </TableCell>
                <TableCell className="whitespace-normal">
                  <BookingGrantPositionList grant={grant} />
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
                  {grant.revocable && (
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
