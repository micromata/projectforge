"use client";

import { useTranslations } from "next-intl";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import type { PositionInvoiceInfo } from "../types";

export interface PositionInvoicesProps {
  invoiceInfo: PositionInvoiceInfo;
  className?: string;
}

/**
 * What a position was already invoiced with: the invoiced and the outstanding sum, and a link per
 * invoice.
 *
 * Read-only throughout, and not part of the form's values: an order position doesn't know its invoices —
 * the invoice positions point at it — so these numbers come from `RechnungCache` with the order
 * (`AuftragsPosition.copyFrom`) and change only when an invoice does. Putting them into the form would
 * make them look editable and be posted back into fields the backend recomputes anyway.
 */
export function PositionInvoices({
  invoiceInfo,
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
        {invoiceInfo.notInvoicedSum != null && (
          <span className="text-muted-foreground">
            {` / ${formatCurrency(invoiceInfo.notInvoicedSum, format)} ${t("fibu.toBeInvoiced")}`}
          </span>
        )}
      </p>
      {invoices.length > 0 && (
        <ul className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-sm">
          {invoices.map((invoice) => (
            <li key={invoice.id ?? invoice.nummer}>
              {/* The invoice's own edit page, hand-built like this one — not the generic
                  `/outgoingInvoice/edit/…` route, which answers `notFound()` for exactly the
                  entities that are hand-built (see the generic route's page-client). Guarded, since
                  this link leaves an order form that may hold unsaved changes. */}
              <GuardedLink
                href={`/invoice/${invoice.id}`}
                className="text-primary underline-offset-2 hover:underline"
                // The number alone doesn't say what the link opens, and there are several of them.
                aria-label={`${t("fibu.rechnung._")} ${invoice.nummer}`}
              >
                {invoice.nummer}
              </GuardedLink>
              <span className="ml-1 text-muted-foreground tabular-nums">
                {formatDate(invoice.date, format)}
                {invoice.netSum != null &&
                  `, ${formatCurrency(invoice.netSum, format)}`}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
