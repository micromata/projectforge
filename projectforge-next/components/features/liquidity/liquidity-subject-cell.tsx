"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { LiquidityListRow } from "./types";
import { isVirtualRow } from "./types";

/**
 * The subject cell of the liquidity list, with a "Series" badge on every row that belongs to a recurring
 * series — a still-virtual, projected occurrence as well as a materialized one — so a reader tells a
 * series entry from a one-off at a glance. A virtual occurrence's badge is outlined (nothing stored yet),
 * a materialized one's is solid.
 *
 * A materialized occurrence carries a second "Fixed" badge beside it: it is a real, stored row frozen out
 * of the series (touched, paid or deleted), so later changes to the series template no longer reach it —
 * a distinction the plain "Series" badge alone did not make (see LiquiditySeriesProjector, copy-on-write).
 */
export function LiquiditySubjectCell({ row }: { row: LiquidityListRow }) {
  const t = useTranslations();
  const virtual = isVirtualRow(row);
  const inSeries = row.seriesId != null;
  return (
    <span className="flex min-w-0 items-center gap-2">
      <span className={cn("truncate", virtual && "italic")}>{row.subject}</span>
      {inSeries && (
        <Badge
          variant={virtual ? "outline" : "secondary"}
          className="shrink-0 text-[10px]"
        >
          {t("plugins.liquidityplanning.series.badge")}
        </Badge>
      )}
      {inSeries && !virtual && (
        <Badge variant="outline" className="shrink-0 text-[10px]">
          {t("plugins.liquidityplanning.series.fixedBadge")}
        </Badge>
      )}
    </span>
  );
}
