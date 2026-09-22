"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";
import { OrderLink } from "@/components/shared/orders/order-link";
import type { OrderRef } from "./types";

/**
 * The orders an invoice bills, as links to them — Wicket's `OrderPositionsPanel` column, reduced to the
 * orders themselves: which position of an order is billed belongs to the invoice's positions and is
 * shown there (see OrderPositionLink), while a reader of the list is after the order this invoice
 * belongs to.
 *
 * Each opens in that order's own tab, so the list keeps its scroll position, its filter and its
 * selection (see [OrderLink]).
 */
export function OrdersCell({ orders }: { orders: OrderRef[] | undefined }) {
  const t = useTranslations();
  if (!orders?.length) return null;
  return (
    // Inline rather than a flex row, so the cell clips with an ellipsis like every other one; the whole
    // list is in the column's tooltip (see invoice.page.tsx).
    <>
      {orders.map((order, index) => (
        <Fragment key={order.id ?? index}>
          {index > 0 && ", "}
          {order.id == null ? (
            // No link without an id — nothing to navigate to.
            (order.nummer ?? "")
          ) : (
            <OrderLink
              orderId={order.id}
              ariaLabel={`${t("show")}: ${t("fibu.auftrag._")} ${order.nummer ?? order.id}`}
            >
              {/* The number, as the order is referred to everywhere. */}
              {order.nummer ?? order.id}
            </OrderLink>
          )}
        </Fragment>
      ))}
    </>
  );
}

/** The numbers of the orders as one string — the column's plain value and its tooltip. */
export function orderNumbers(orders: OrderRef[] | undefined) {
  return (orders ?? []).map((order) => order.nummer ?? order.id).join(", ");
}
