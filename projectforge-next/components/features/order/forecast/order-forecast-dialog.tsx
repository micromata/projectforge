"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ChartLineData01Icon } from "@hugeicons/core-free-icons";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { OrderForecastPanel } from "./order-forecast-panel";

/** The order a forecast analysis is shown for: its id and the number shown in the title. */
export type ForecastOrderRef = { id: number; label: string };

/**
 * The forecast analysis of one order (the content of its Forecast tab) above the page it was opened from,
 * e.g. the tables of the forecast statistics, which so keep their sub-tab, filters and scroll position.
 * The order number in the title opens the order at its Forecast tab.
 */
export function OrderForecastDialog({
  order,
  onClose,
}: {
  order: ForecastOrderRef | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  return (
    <Dialog open={order !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex max-h-[90vh] flex-col overflow-y-auto sm:max-w-6xl">
        {order && (
          <>
            <DialogHeader>
              <DialogTitle className="pr-6">
                {t("fibu.auftrag.forecast._")} – {t("fibu.auftrag._")}{" "}
                <Link
                  href={`/order/${order.id}?tab=forecast`}
                  className="hover:underline"
                >
                  {order.label}
                </Link>
              </DialogTitle>
            </DialogHeader>
            <OrderForecastPanel id={order.id} savedOnlyHint={false} />
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

/**
 * The pill behind an order number in a table that opens [OrderForecastDialog], tinted with the info tone of
 * `StatusPill` so it stands out from the cell text in both themes. It stops the click, so a row click
 * handler of the table doesn't fire too.
 */
export function OrderForecastButton({
  order,
  label,
  onOpen,
}: {
  order: ForecastOrderRef;
  /** Tooltip and accessible name. */
  label: string;
  onOpen: (order: ForecastOrderRef) => void;
}) {
  return (
    <HintTooltip text={label}>
      <button
        type="button"
        aria-label={label}
        className="inline-flex shrink-0 items-center rounded-full border px-1.5 py-px transition-[filter] hover:brightness-125 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        style={{
          background: "var(--status-info-bg)",
          color: "var(--status-info)",
          borderColor: "var(--status-info-border)",
        }}
        onClick={(event) => {
          event.stopPropagation();
          onOpen(order);
        }}
      >
        <HugeiconsIcon icon={ChartLineData01Icon} size={13} aria-hidden />
      </button>
    </HintTooltip>
  );
}
