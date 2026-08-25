"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Invoice01Icon } from "@hugeicons/core-free-icons";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import type { PositionInvoiceInfo } from "../types";

export interface PositionInvoicesProps {
  invoiceInfo: PositionInvoiceInfo;
  /**
   * Whether the invoice number links to the invoice's own page. False renders it as plain text — the
   * number is shown to every reader, but only a user with select access on outgoing invoices could open
   * it, exactly as Wicket's `InvoicePositionsPanel` gates its link.
   */
  canOpenInvoice: boolean;
  className?: string;
}

/**
 * What a position was already invoiced with: the invoiced and the outstanding sum, and one entry per
 * invoice — the number (a link for the finance staff, plain text for everybody else), its date and net
 * sum.
 *
 * Read-only throughout, and not part of the form's values: an order position doesn't know its invoices —
 * the invoice positions point at it — so these numbers come from `RechnungCache` with the order
 * (`AuftragsPosition.copyFrom`) and change only when an invoice does. Putting them into the form would
 * make them look editable and be posted back into fields the backend recomputes anyway.
 */
export function PositionInvoices({
  invoiceInfo,
  canOpenInvoice,
  className,
}: PositionInvoicesProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const invoices = invoiceInfo.invoices ?? [];

  return (
    <div className={className}>
      <p className="text-xs font-medium text-muted-foreground">
        {t("fibu.rechnungen")}
      </p>
      <p className="mt-1 text-sm tabular-nums">
        {formatCurrency(invoiceInfo.invoicedSum, format)}
        {/* „noch nicht fakturiert", not „zu fakturieren": this is `OrderPositionInfo.notYetInvoiced` —
            the commissioned amount not yet billed (an information) — not the part of it that is due now
            (the to-do). See `AuftragsPosition.notInvoicedSum` and order-statistics.ts. */}
        {invoiceInfo.notInvoicedSum != null && (
          <span className="text-muted-foreground">
            {` / ${formatCurrency(invoiceInfo.notInvoicedSum, format)} ${t("fibu.notYetInvoiced")}`}
          </span>
        )}
      </p>
      {invoices.length > 0 && (
        // A small table: icon, number, date and net sum each in their own column, so the numbers, dates
        // and amounts of the several invoices of a position line up under each other. `display: contents`
        // lets every row's cells take part in the one grid (a nested table would align nothing).
        <ul className="mt-1 grid w-fit grid-cols-[auto_auto_auto_auto] items-center gap-x-2 gap-y-0.5 text-sm tabular-nums">
          {invoices.map((invoice) => (
            <li key={invoice.id ?? invoice.nummer} className="contents">
              <HugeiconsIcon
                icon={Invoice01Icon}
                size={12}
                className="shrink-0 text-muted-foreground"
              />
              {canOpenInvoice ? (
                /* The invoice's own edit page, hand-built like this one — not the generic
                   `/outgoingInvoice/edit/…` route, which answers `notFound()` for exactly the
                   entities that are hand-built (see the generic route's page-client). Guarded, since
                   this link leaves an order form that may hold unsaved changes. */
                <GuardedLink
                  href={`/invoice/${invoice.id}`}
                  className="text-primary underline-offset-2 hover:underline"
                  // The number alone doesn't say what the link opens, and there are several of them.
                  aria-label={`${t("fibu.rechnung._")} ${invoice.nummer}`}
                >
                  {`#${invoice.nummer}`}
                </GuardedLink>
              ) : (
                /* No select access on outgoing invoices: the number is still shown (a reader of the
                   order sees which invoices bill it), but not as a link the invoice page would refuse
                   to open — the same as Wicket's InvoicePositionsPanel renders for a non-finance user. */
                <span>{`#${invoice.nummer}`}</span>
              )}
              <span className="text-muted-foreground">
                {invoice.date ? formatDate(invoice.date, format) : ""}
              </span>
              <span className="text-right text-muted-foreground">
                {invoice.netSum != null
                  ? formatCurrency(invoice.netSum, format)
                  : ""}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
