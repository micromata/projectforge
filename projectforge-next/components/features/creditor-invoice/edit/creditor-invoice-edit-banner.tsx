"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { InvoiceSumsLine } from "@/components/shared/invoice/invoice-sums-line";
import { paidState } from "@/components/shared/invoice/paid-state";
import { StatusPill } from "@/components/shared/status-pill";
import { EINGANGSRECHNUNG_METADATA } from "@/lib/metadata/eingangsrechnung.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { cn } from "@/lib/utils";
import type { CreditorInvoiceValues } from "../creditor-invoice-schema";

const m = fromMetadata(EINGANGSRECHNUNG_METADATA);

/**
 * Sticky banner between the tab strip and the scrollable sections — stays in view while the user scrolls
 * through the positions.
 *
 * Leaner than the outgoing invoice's banner: a creditor invoice has no number, so what identifies it here is
 * its creditor and reference. Beside them it badges the two states it does have — its paid status (paid,
 * overdue or open, coloured like the list) and its payment type — and the live running sums, so the reader
 * never has to scroll back to the head section to check what they are editing.
 */
export function CreditorInvoiceEditBanner() {
  const t = useTranslations();
  const form = useEntityEditForm();

  // Subscribe only to the fields the banner shows, so it doesn't re-render on every keystroke.
  const { kreditor, referenz, bezahlDatum, faelligkeit, paymentType } =
    useStore(
      form.store,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (state: any) => {
        const v = state.values as CreditorInvoiceValues;
        return {
          kreditor: v.kreditor,
          referenz: v.referenz,
          bezahlDatum: v.bezahlDatum,
          faelligkeit: v.faelligkeit,
          paymentType: v.paymentType,
        };
      }
    );

  const identifier = [kreditor, referenz].filter(Boolean).join(" · ");
  const paid = paidState(bezahlDatum, faelligkeit);
  const paymentTypeLabel = m
    .enumOptions("paymentType", t)
    .find((o) => o.value === paymentType)?.label;

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b bg-background px-6 py-2">
      <div className="flex shrink-0 items-center gap-2">
        {identifier && (
          <span className={cn("text-sm font-semibold")}>{identifier}</span>
        )}
        <StatusPill tone={paid.tone} label={t(paid.labelKey)} />
        {/* Not a status, so no colour — the neutral outline the outgoing invoice gives its type. */}
        {paymentTypeLabel && (
          <Badge variant="outline" className="font-normal">
            {paymentTypeLabel}
          </Badge>
        )}
      </div>
      <InvoiceSumsLine
        entity="incomingInvoice"
        className="ml-auto justify-end"
      />
    </div>
  );
}
