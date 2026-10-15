"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * A link to another outgoing invoice, opened in a tab of its own — the invoice a cancellation cancels, or
 * the cancellation an invoice is cancelled by. A named tab rather than the current one for the reasons
 * [OrderLink] gives: the invoice being edited stays open, and following the same link twice focuses the tab
 * already showing it.
 */
export function InvoiceLink({
  invoiceId,
  children,
  className,
  ariaLabel,
}: {
  invoiceId: number;
  children: ReactNode;
  className?: string;
  ariaLabel?: string;
}) {
  return (
    <Link
      href={`/invoice/${invoiceId}`}
      target={`pf-invoice-${invoiceId}`}
      className={cn(
        "text-primary underline-offset-2 hover:underline",
        className
      )}
      aria-label={ariaLabel}
      onClick={(event) => event.stopPropagation()}
    >
      {children}
    </Link>
  );
}
