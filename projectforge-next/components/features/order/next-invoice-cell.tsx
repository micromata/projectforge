"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type { OrderListRow } from "./types";

/**
 * When the next invoice of an order is to be written: „sofort" for a finished position (or a reached
 * payment schedule without a date), otherwise the date of the earliest reached payment schedule — which
 * may lie in a following month, if the project team marked it reached early. Empty if nothing is to be
 * invoiced.
 */
export function NextInvoiceCell({ row }: { row: OrderListRow }) {
  const t = useTranslations();
  const format = useFormatContext();
  if (row.toBeInvoicedImmediately)
    return <>{t("fibu.auftrag.nextInvoice.immediately")}</>;
  return <>{formatDate(row.nextInvoiceDate, format)}</>;
}
