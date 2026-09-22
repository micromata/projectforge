"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDateRange,
  formatNumber,
  formatPercentageDecimal,
} from "@/lib/format";
import { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

const p = fromMetadata(RECHNUNGS_POSITION_METADATA);

/**
 * Every field of a position except its text and its net sum, as short labelled chips — what a folded row
 * shows so that nothing has to be unfolded to be read.
 *
 * Labelled rather than bare, unlike the order's header: an invoice position carries three amounts (net,
 * VAT, gross) and two quantities, and four numbers in a row would be unreadable without saying which is
 * which. The wording is the entity's own (`fibu.common.*`, the same terms the open row uses).
 *
 * A hook rather than a function so it can reach the user's locale and the catalogue; separated from
 * [PositionRowHeader] only to keep either file readable.
 */
export function usePositionDetailChips(
  position: InvoicePositionValues,
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

  // The type always, the dates only where the position has its own: "see above" is what tells a reader
  // that the invoice's period applies here, which is a fact about the position and not a default to hide.
  const periodType = p
    .enumOptions("periodOfPerformanceType", t)
    .find((option) => option.value === position.periodOfPerformanceType);
  if (periodType) {
    const dates =
      position.periodOfPerformanceType === "OWN"
        ? formatDateRange(
            position.periodOfPerformanceBegin,
            position.periodOfPerformanceEnd,
            format
          )
        : "";
    chips.push({
      label: t("fibu.periodOfPerformance._"),
      value: dates || periodType.label,
    });
  }

  return chips;
}
