"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserTime01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type { TaskBookingGrantBadgeEntry } from "@/lib/rs/task";

/**
 * Marks a node of the structure tree that has current ad hoc booking grants: their number, and in the
 * tooltip who may book on it until when. Sent by the backend only to who may grant on the node.
 */
export function TaskBookingGrantBadge({
  grants,
}: {
  grants: TaskBookingGrantBadgeEntry[];
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
    })`;
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
