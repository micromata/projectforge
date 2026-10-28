"use client";

import { useTranslations } from "next-intl";
import type { TaskBookingGrant } from "@/lib/rs/task-booking-grant";

/**
 * The structure elements of a grant, each with its cost units, and how many more it has that the logged-in
 * user doesn't see.
 */
export function BookingGrantPositionList({
  grant,
}: {
  grant: TaskBookingGrant;
}) {
  const t = useTranslations();
  return (
    <ul className="flex flex-col gap-1">
      {grant.positions.map((position) => (
        <li key={position.taskId}>
          <span>{position.path}</span>
          <span className="block text-xs text-muted-foreground">
            {position.kost2Titles?.join(", ") ??
              t("task.bookingGrant.kost2.all")}
          </span>
        </li>
      ))}
      {grant.hiddenPositions > 0 && (
        <li className="text-xs text-muted-foreground">
          {t("task.bookingGrant.positions.hidden", {
            arg0: grant.hiddenPositions,
          })}
        </li>
      )}
    </ul>
  );
}
