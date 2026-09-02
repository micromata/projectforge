"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { InvoiceValues } from "../invoice-schema";
import { referencedOrders } from "../invoice-values";
import { OrdersCell } from "../orders-cell";

/**
 * The orders this invoice bills, as links, in the sticky edit banner — the "Aufträge" the list column
 * shows (see OrdersCell), now beside the running sums so the reader sees which orders the invoice belongs
 * to without scrolling through the positions.
 *
 * Derived from the positions in the form, not from a DTO field: each position carries the order it bills
 * ([referencedOrders]), so the list follows the picker live. Subscribes through a compact signature of
 * the referenced orders so it re-renders only when that set changes — not on every keystroke in a
 * position, the same restraint the banner keeps for its badges.
 */
export function ReferencedOrders({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();

  const signature = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) =>
      referencedOrders((state.values as InvoiceValues).positionen)
        .map((order) => `${order.id}:${order.nummer ?? ""}`)
        .join(",")
  );
  if (!signature) return null;

  const orders = signature.split(",").map((entry) => {
    const [id, nummer] = entry.split(":");
    return {
      id: Number(id),
      nummer: nummer === "" ? undefined : Number(nummer),
    };
  });

  return (
    <div className={cn("flex min-w-0 flex-col", className)}>
      <span className="text-[11px] opacity-70">
        {t("fibu.auftrag.auftraege")}
      </span>
      <span className="truncate text-sm">
        <OrdersCell orders={orders} />
      </span>
    </div>
  );
}
