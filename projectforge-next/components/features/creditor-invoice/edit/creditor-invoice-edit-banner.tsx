"use client";

import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { InvoiceSumsLine } from "@/components/shared/invoice/invoice-sums-line";
import { cn } from "@/lib/utils";
import type { CreditorInvoiceValues } from "../creditor-invoice-schema";

/**
 * Sticky banner between the tab strip and the scrollable sections — stays in view while the user scrolls
 * through the positions.
 *
 * Leaner than the outgoing invoice's banner: a creditor invoice has no number, status or type to badge, so
 * what identifies it here is its creditor and reference, beside the live running sums — the reader never has
 * to scroll back to the head section to check what they are editing.
 */
export function CreditorInvoiceEditBanner() {
  const form = useEntityEditForm();

  // Subscribe only to the two identifiers so the banner doesn't re-render on every keystroke.
  const { kreditor, referenz } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as CreditorInvoiceValues;
      return { kreditor: v.kreditor, referenz: v.referenz };
    }
  );

  const identifier = [kreditor, referenz].filter(Boolean).join(" · ");

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b bg-background px-6 py-2">
      {identifier && (
        <span className={cn("shrink-0 text-sm font-semibold")}>
          {identifier}
        </span>
      )}
      <InvoiceSumsLine
        entity="incomingInvoice"
        className="ml-auto justify-end"
      />
    </div>
  );
}
