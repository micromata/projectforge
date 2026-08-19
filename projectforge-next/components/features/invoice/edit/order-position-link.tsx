"use client";

import { useTranslations } from "next-intl";
import { GuardedLink } from "@/components/shared/guarded-link";
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
    <GuardedLink
      href={`/order/${order.auftragId}`}
      className={cn(
        "shrink-0 text-primary underline-offset-2 hover:underline",
        className
      )}
      aria-label={`${t("show")}: ${label}`}
    >
      {label}
    </GuardedLink>
  );
}
