"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { BooleanCell } from "@/components/data-table/cells/boolean-cell";
import type { LiquidityListRow } from "./types";

/**
 * The paid status of a liquidity entry — three-valued, because `paid` is: a tick when the entry is
 * effectively paid (the backend's `effectivePaid`: a manual `paid=true`, or the auto rule once the date
 * of payment has passed), a muted "automatic" hint while the entry is still open but on the auto rule
 * (`paid` null and `autoSetPaid` set, date not yet passed), and nothing for a plainly unpaid entry.
 *
 * The truth stays the server's `effectivePaid`; this cell only adds the "will auto-resolve" nuance the
 * plain tick would hide, read from the raw `paid` / `autoSetPaid` the list row already carries.
 */
export function LiquidityPaidCell({ row }: { row: LiquidityListRow }) {
  const t = useTranslations();
  if (row.effectivePaid) {
    return <BooleanCell value={true} t={t} />;
  }
  if (row.paid == null && row.autoSetPaid) {
    return (
      <HintTooltip text={t("plugins.liquidityplanning.entry.autoSetPaid.info")}>
        <span className="text-[11px] text-muted-foreground">
          {t("plugins.liquidityplanning.entry.paid.automatic")}
        </span>
      </HintTooltip>
    );
  }
  // Plainly unpaid: nothing visible, but BooleanCell keeps the sr-only "no" a screen reader needs.
  return <BooleanCell value={false} t={t} />;
}
