"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { AUFTRAG_METADATA } from "@/lib/metadata/auftrag.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { OrderSumsLine } from "./order-sums-line";
import type { OrderValues } from "../order-schema";

const m = fromMetadata(AUFTRAG_METADATA);

/**
 * Sticky banner between the tab strip and the scrollable sections — stays in view while the user
 * scrolls through positions or payment schedules.
 *
 * Shows the order number, its status badge, its forecast type badge and the live running sums so the
 * reader never has to scroll back to the head section to check what they are editing.
 */
export function OrderEditBanner() {
  const t = useTranslations();
  const form = useEntityEditForm();

  // Subscribe only to the three identifiers so the banner doesn't re-render on every keystroke.

  const { nummer, status, forecastType } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as OrderValues;
      return {
        nummer: v.nummer,
        status: v.status,
        forecastType: v.forecastType,
      };
    }
  );

  const statusLabel = m
    .enumOptions("status", t)
    .find((o) => o.value === status)?.label;

  const forecastTypeLabel = m
    .enumOptions("forecastType", t)
    .find((o) => o.value === forecastType)?.label;

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b bg-background px-6 py-2">
      <div className="flex shrink-0 items-center gap-2">
        {nummer != null && (
          <span className="text-sm font-semibold tabular-nums">#{nummer}</span>
        )}
        {statusLabel && (
          <Badge variant="secondary" className="font-normal">
            {statusLabel}
          </Badge>
        )}
        {forecastTypeLabel && (
          <Badge variant="outline" className="font-normal">
            {forecastTypeLabel}
          </Badge>
        )}
      </div>
      <OrderSumsLine className="ml-auto justify-end" />
    </div>
  );
}
