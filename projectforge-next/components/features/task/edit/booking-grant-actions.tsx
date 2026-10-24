"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { TaskBookingGrant } from "@/lib/rs/task-booking-grant";
import { timesheetsToAcceptRoute } from "@/lib/timesheet-links";

/**
 * The status of a grant, for a proposed one (`acceptanceRequired`) with a mark and the number of its time
 * sheets still to be accepted, linked to the list of them.
 */
export function BookingGrantStatus({ grant }: { grant: TaskBookingGrant }) {
  const t = useTranslations();
  return (
    <div className="flex flex-wrap items-center gap-1">
      <Badge variant={grant.status === "ACTIVE" ? "default" : "secondary"}>
        {grant.statusAsString}
      </Badge>
      {grant.acceptanceRequired && (
        <HintTooltip text={t("task.bookingGrant.proposed.tooltip")} openOnTap>
          <Badge variant="outline">{t("task.bookingGrant.proposed._")}</Badge>
        </HintTooltip>
      )}
      {grant.pendingTimesheets > 0 && (
        <Link
          href={timesheetsToAcceptRoute(grant.userId, grant.userName)}
          className="text-xs underline underline-offset-2"
        >
          {t("task.bookingGrant.pending._", { arg0: grant.pendingTimesheets })}
        </Link>
      )}
    </div>
  );
}

/**
 * Confirming (a proposed grant, by who may grant on all of its tasks) and revoking, as the backend allows
 * (`confirmable`, `revocable`).
 */
export function BookingGrantActions({
  grant,
  onConfirm,
  onRevoke,
}: {
  grant: TaskBookingGrant;
  onConfirm: (grant: TaskBookingGrant) => void;
  onRevoke: (grant: TaskBookingGrant) => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex justify-end gap-2">
      {grant.confirmable && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          aria-label={`${t("task.bookingGrant.confirm._")}: ${grant.userName ?? ""}`}
          onClick={() => onConfirm(grant)}
        >
          {t("task.bookingGrant.confirm._")}
        </Button>
      )}
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
    </div>
  );
}
