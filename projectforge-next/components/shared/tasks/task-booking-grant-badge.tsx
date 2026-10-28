"use client";

import Link from "next/link";
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
 *
 * With [href] (the task form at its grants), a click on it leads there: the place to manage the grants and
 * to accept the time sheets.
 */
export function TaskBookingGrantBadge({
  grants,
  pendingTimesheets,
  href,
}: {
  grants?: TaskBookingGrantBadgeEntry[];
  pendingTimesheets?: number;
  href?: string;
}) {
  // A link opens on a tap, the explanation then on hover only (a tap opening both would hide it at once).
  const openOnTap = !href;
  const badges = (
    <>
      {grants && grants.length > 0 && (
        <GrantsBadge grants={grants} openOnTap={openOnTap} />
      )}
      {pendingTimesheets != null && pendingTimesheets > 0 && (
        <PendingBadge count={pendingTimesheets} openOnTap={openOnTap} />
      )}
    </>
  );
  return href ? (
    <Link href={href} className="inline-flex gap-1">
      {badges}
    </Link>
  ) : (
    <span className="inline-flex gap-1">{badges}</span>
  );
}

function PendingBadge({
  count,
  openOnTap,
}: {
  count: number;
  openOnTap: boolean;
}) {
  const t = useTranslations();
  const text = t("task.bookingGrant.pending.tooltip", { arg0: count });
  return (
    <HintTooltip text={text} openOnTap={openOnTap}>
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

function GrantsBadge({
  grants,
  openOnTap,
}: {
  grants: TaskBookingGrantBadgeEntry[];
  openOnTap: boolean;
}) {
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
    <HintTooltip
      title={title}
      text={lines.join("\n")}
      plain
      openOnTap={openOnTap}
    >
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
