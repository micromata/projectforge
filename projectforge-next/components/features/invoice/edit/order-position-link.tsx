"use client";

import { useTranslations } from "next-intl";
import { OrderLink } from "@/components/shared/orders/order-link";
import { cn } from "@/lib/utils";
import type { InvoicePositionValues } from "../invoice-schema";

/**
 * The order position an invoice position bills, as a link to its order — Wicket's `IconPanel(GOTO)` next
 * to the picker, and the same reference the collapsed row header names.
 *
 * Its own file because both places show it: the header of a folded row (see PositionRowHeader) and the
 * picker of an unfolded one (see OrderPositionField). Renders nothing without an order id: a reference
 * from an order the user may not read arrives without one, and so does a position just picked in a form
 * whose hit carried none.
 *
 * Which position is billed is what it says, and the order is where it leads — the position has no page
 * of its own. Opens in the order's own tab, so the form it sits in survives the click (see [OrderLink]).
 */
export function OrderPositionLink({
  order,
  className,
}: {
  order: InvoicePositionValues["auftragsPosition"];
  className?: string;
}) {
  const t = useTranslations();
  if (order?.auftragId == null) return null;
  const label = `${t("fibu.auftrag._")} ${order.auftragNummer ?? ""}.${order.number ?? ""}`;
  return (
    <OrderLink
      orderId={order.auftragId}
      className={cn("shrink-0", className)}
      ariaLabel={`${t("show")}: ${label}`}
    >
      {label}
    </OrderLink>
  );
}
