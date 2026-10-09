"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type { LogGroupCounts } from "@/lib/rs/admin-errors";

/**
 * The handled part of a figure below its active count: "+ 120 muted · 3 ignored · 40 resolved", only the parts
 * that aren't 0. Nothing, if nothing is handled.
 */
export function AdminHandledCounts({ counts }: { counts: LogGroupCounts }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  // The keys are spelled out: the i18n scan of `gen` finds literal keys only.
  const parts = [
    counts.muted > 0 &&
      t("system.admin.adminErrors.handled.muted", {
        arg0: formatNumber(counts.muted, ctx, 0),
      }),
    counts.ignored > 0 &&
      t("system.admin.adminErrors.handled.ignored", {
        arg0: formatNumber(counts.ignored, ctx, 0),
      }),
    counts.resolved > 0 &&
      t("system.admin.adminErrors.handled.resolved", {
        arg0: formatNumber(counts.resolved, ctx, 0),
      }),
  ].filter(Boolean);
  if (parts.length === 0) {
    return null;
  }
  return (
    <div className="text-xs text-muted-foreground tabular-nums">
      + {parts.join(" · ")}
    </div>
  );
}
