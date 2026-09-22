"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatNumber,
  formatPercentageDecimal,
} from "@/lib/format";
import type { CreditorInvoicePositionValues } from "../creditor-invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice-sums";

/**
 * Every field of a position except its text and its net sum, as short labelled chips — what a folded row
 * shows so that nothing has to be unfolded to be read.
 *
 * The same idea as the outgoing invoice's [usePositionDetailChips], minus the order link and the period of
 * performance the incoming position does not have: quantity × unit price, the VAT rate and the two amounts
 * it produces.
 */
export function usePositionDetailChips(
  position: CreditorInvoicePositionValues,
  sums: InvoicePositionSums | undefined
): { label?: string; value: string }[] {
  const t = useTranslations();
  const format = useFormatContext();
  const chips: { label?: string; value: string }[] = [];

  if (position.menge != null || position.einzelNetto != null) {
    chips.push({
      value: `${formatNumber(position.menge, format, 2)} × ${formatCurrency(position.einzelNetto, format)}`,
    });
  }
  // The rate as a percentage, from the factor the entity holds (0.19 → "19 %"), beside the amount it
  // produced — the open row shows both, and the rate alone doesn't say what was charged.
  if (position.vat != null) {
    chips.push({
      label: t("fibu.common.vat"),
      value: formatPercentageDecimal(position.vat, format),
    });
  }
  if (sums?.vatAmount != null) {
    chips.push({
      label: t("fibu.common.vatAmount"),
      value: formatCurrency(sums.vatAmount, format),
    });
  }
  if (sums?.grossSum != null) {
    chips.push({
      label: t("fibu.common.brutto"),
      value: formatCurrency(sums.grossSum, format),
    });
  }

  return chips;
}
