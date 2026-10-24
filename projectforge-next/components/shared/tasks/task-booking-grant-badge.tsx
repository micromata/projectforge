"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { TaskDone01Icon, UserTime01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type { TaskBookingGrantBadgeEntry } from "@/lib/rs/task";

/**
 * Marks a node of the structure tree that has current ad hoc booking grants: their number, and in the
 * tooltip who may book on it until when. Next to it the number of time sheets still to be accepted (booked
 * by a grant proposed without the right to grant). Sent by the backend only to who may grant on the node.
 */
export function TaskBookingGrantBadge({
  grants,
  pendingTimesheets,
}: {
  grants?: TaskBookingGrantBadgeEntry[];
  pendingTimesheets?: number;
}) {
  return (
    <span className="inline-flex gap-1">
      {grants && grants.length > 0 && <GrantsBadge grants={grants} />}
      {pendingTimesheets != null && pendingTimesheets > 0 && (
        <PendingBadge count={pendingTimesheets} />
      )}
    </span>
  );
}

function PendingBadge({ count }: { count: number }) {
  const t = useTranslations();
  const text = t("task.bookingGrant.pending.tooltip", { arg0: count });
  return (
    <HintTooltip text={text} openOnTap>
      <Badge
        variant="outline"
        className="gap-1 text-xs"
        style={{
          background: "var(--status-loaned-bg)",
          color: "var(--status-loaned)",
          borderColor: "var(--status-loaned-border)",
        }}
        aria-label={text}
      >
        <HugeiconsIcon icon={TaskDone01Icon} size={12} aria-hidden />
        {count}
      </Badge>
    </HintTooltip>
  );
}

function GrantsBadge({ grants }: { grants: TaskBookingGrantBadgeEntry[] }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const title = t("task.bookingGrant.badge._", { arg0: grants.length });
  const lines = grants.map((grant) => {
    const period = `${formatDate(grant.validFrom, ctx)} – ${formatDate(grant.validUntil, ctx)}`;
    return `${grant.userName ?? ""} (${period}${
      grant.upcoming
        ? `, ${t("task.bookingGrant.badge.upcoming", { arg0: formatDate(grant.validFrom, ctx) })}`
        : ""
    }${grant.acceptanceRequired ? `, ${t("task.bookingGrant.proposed._")}` : ""})`;
  });
  return (
    <HintTooltip title={title} text={lines.join("\n")} plain openOnTap>
      <Badge
        variant="outline"
        className="gap-1 text-xs"
        style={{
          background: "var(--status-info-bg)",
          color: "var(--status-info)",
          borderColor: "var(--status-info-border)",
        }}
        aria-label={`${title}: ${lines.join(", ")}`}
      >
        <HugeiconsIcon icon={UserTime01Icon} size={12} aria-hidden />
        {grants.length}
      </Badge>
    </HintTooltip>
  );
}
