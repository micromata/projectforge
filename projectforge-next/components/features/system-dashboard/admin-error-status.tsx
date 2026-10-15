"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { NotificationOff01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { StatusPill } from "@/components/shared/status-pill";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";
import { STATUS_KEYS, statusTone } from "./admin-errors-labels";

/** The status of a problem: a regression stands out as such, a muted one carries a crossed-out bell. */
export function AdminErrorStatus({ entry }: { entry: LogGroupEntry }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <div className="flex flex-wrap items-center gap-1">
      <StatusPill
        tone={statusTone(entry)}
        label={
          entry.regression
            ? t("system.admin.adminErrors.regression")
            : t(STATUS_KEYS[entry.status])
        }
      />
      {entry.mutedUntil != null && (
        <HintTooltip
          plain
          openOnTap
          text={t("system.admin.adminErrors.mutedUntil", {
            arg0: formatTimestampMinutes(entry.mutedUntil, ctx),
          })}
        >
          <span className="text-muted-foreground">
            <HugeiconsIcon icon={NotificationOff01Icon} size={14} />
          </span>
        </HintTooltip>
      )}
    </div>
  );
}
