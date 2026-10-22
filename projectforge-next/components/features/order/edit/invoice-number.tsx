"use client";

import { useTranslations } from "next-intl";
import { GuardedLink } from "@/components/shared/guarded-link";
import { cn } from "@/lib/utils";
import type { InvoiceRef } from "../types";

export interface InvoiceNumberProps {
  invoice: InvoiceRef;
  /**
   * Whether the number links to the invoice's own page — the select access on outgoing invoices. False
   * shows it as plain text, as Wicket's `InvoicePositionsPanel` does for a non-finance reader.
   */
  canOpenInvoice: boolean;
  /** Put in front of the number, e.g. `#`. */
  prefix?: string;
}

/**
 * The number of an invoice of an order position, as shown in the position's header and in its invoice
 * list. A cancellation has no number of its own, only its printed one (e.g. 17400-S); a cancelled invoice
 * and its cancellation count 0 together, so both are struck through.
 */
export function InvoiceNumber({
  invoice,
  canOpenInvoice,
  prefix = "",
}: InvoiceNumberProps) {
  const t = useTranslations();
  const number = invoice.belegNummer ?? invoice.nummer;
  const struck = invoice.cancelled && "line-through";
  if (!canOpenInvoice) {
    // The number is still shown (a reader of the order sees which invoices bill it), but not as a link
    // the invoice page would refuse to open.
    return <span className={cn(struck)}>{`${prefix}${number}`}</span>;
  }
  return (
    /* `/invoice/{id}`, not `/outgoingInvoice/edit/{id}`: the generic route answers notFound() for a
       hand-built entity. Guarded, since this link leaves an order form that may hold unsaved changes. */
    <GuardedLink
      href={`/invoice/${invoice.id}`}
      className={cn("text-primary underline-offset-2 hover:underline", struck)}
      // The number alone doesn't say what the link opens, and there may be several of them.
      aria-label={`${t("fibu.rechnung._")} ${number}`}
    >
      {`${prefix}${number}`}
    </GuardedLink>
  );
}
