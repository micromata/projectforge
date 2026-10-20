"use client";

import { useFormatContext } from "@/hooks/use-format";
import { formatNumberUpTo } from "@/lib/format";

/** The digits a conversion rate is stored with (`CurrencyConversionRateDO`, `scale = 8`). */
export const RATE_FRACTION_DIGITS = 8;

/** A conversion rate in the list, with its own digits (a DECIMAL column would round it to two). */
export function CurrencyPairRateCell({ value }: { value: unknown }) {
  const ctx = useFormatContext();
  return (
    <span className="font-mono tabular-nums">
      {formatNumberUpTo(value, ctx, RATE_FRACTION_DIGITS)}
    </span>
  );
}
