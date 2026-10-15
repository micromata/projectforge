"use client";

import { useTranslations } from "next-intl";
import { Fragment } from "react";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type { LogGroupCounts } from "@/lib/rs/admin-errors";

/** The status filters of the handled parts, see [AdminHandledCounts]. */
export type HandledStatus = "MUTED" | "IGNORED" | "RESOLVED";

/**
 * The handled part of a figure below its active count: "+ 120 muted · 3 ignored · 40 resolved", only the parts
 * that aren't 0. Nothing, if nothing is handled. With [onOpen] each part is a button showing its problems; it lies
 * above the tile's own button (see [TILE_BUTTON_CLASS]).
 */
export function AdminHandledCounts({
  counts,
  onOpen,
}: {
  counts: LogGroupCounts;
  onOpen?: (status: HandledStatus) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  // The keys are spelled out: the i18n scan of `gen` finds literal keys only.
  const parts = [
    counts.muted > 0 && {
      status: "MUTED" as HandledStatus,
      label: t("system.admin.adminErrors.handled.muted", {
        arg0: formatNumber(counts.muted, ctx, 0),
      }),
    },
    counts.ignored > 0 && {
      status: "IGNORED" as HandledStatus,
      label: t("system.admin.adminErrors.handled.ignored", {
        arg0: formatNumber(counts.ignored, ctx, 0),
      }),
    },
    counts.resolved > 0 && {
      status: "RESOLVED" as HandledStatus,
      label: t("system.admin.adminErrors.handled.resolved", {
        arg0: formatNumber(counts.resolved, ctx, 0),
      }),
    },
  ].filter((it) => it !== false);
  if (parts.length === 0) {
    return null;
  }
  return (
    <div className="text-xs text-muted-foreground tabular-nums">
      +{" "}
      {parts.map((part, index) => (
        <Fragment key={part.status}>
          {index > 0 && " · "}
          {onOpen ? (
            <button
              type="button"
              className="relative z-10 rounded-sm hover:text-foreground hover:underline"
              onClick={() => onOpen(part.status)}
            >
              {part.label}
            </button>
          ) : (
            part.label
          )}
        </Fragment>
      ))}
    </div>
  );
}

/**
 * The button of a tile with [AdminHandledCounts]: it covers the whole tile (which is `relative`), so that a click
 * anywhere opens the tile, while the handled parts lie above it. A button can't hold the parts' buttons.
 */
export const TILE_BUTTON_CLASS =
  "absolute inset-0 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";
