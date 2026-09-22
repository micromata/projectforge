"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * A link to an order's page, opened in a tab of its own — one tab per order.
 *
 * Every place pointing at an order from somewhere else uses it: the order of an invoice position
 * (OrderPositionLink, in the form and in a folded position's header) and the orders column of the
 * invoice list. A new tab, because both sit in something the reader wants to keep — an invoice being
 * edited, or a list scrolled to a row — and because an order is usually read *beside* the invoice it
 * belongs to, not instead of it.
 *
 * The tab is **named after the order** rather than `_blank`, so following the same order twice focuses
 * the tab already showing it instead of piling up copies of it (the browser reuses a window by name).
 * That is also why there is no `rel="noopener"`: it makes the browser ignore the name — the HTML
 * specification turns a named target into `_blank` then — and the target is our own origin, where an
 * opener is no risk.
 *
 * No unsaved-changes prompt (GuardedLink) is needed: the form stays where it is, in its own tab.
 */
export function OrderLink({
  orderId,
  children,
  className,
  ariaLabel,
}: {
  orderId: number;
  children: ReactNode;
  className?: string;
  /** What the link says out of context, where its text is only a number. */
  ariaLabel?: string;
}) {
  return (
    <Link
      href={`/order/${orderId}`}
      target={orderTabName(orderId)}
      className={cn(
        "text-primary underline-offset-2 hover:underline",
        className
      )}
      aria-label={ariaLabel}
      // The click stays with the link: every place it sits in reacts to a click of its own — a list row
      // opens the invoice for editing, the header of a folded position unfolds it — and following the
      // link is not that.
      onClick={(event) => event.stopPropagation()}
    >
      {children}
    </Link>
  );
}

/** Name of the tab an order is shown in — the same for every link to that order. */
function orderTabName(orderId: number) {
  return `pf-order-${orderId}`;
}
