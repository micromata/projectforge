import type { LiquiditySeriesValues } from "./liquidity-series-schema";
import type { LiquiditySeriesDetail } from "./types";

/**
 * Normalises the loaded series into the form's values, so no field ever holds `undefined` (see
 * liquidity-values.ts for why). The mandatory `subject` keeps "" rather than null; `frequency` defaults
 * to MONTHLY and `intervalMonths` to 1 — the only recurrence the editor offers today.
 */
export function toSeriesFormValues(
  series: LiquiditySeriesDetail
): LiquiditySeriesValues {
  return {
    id: series.id ?? null,
    startDate: series.startDate ?? null,
    frequency: series.frequency ?? "MONTHLY",
    intervalMonths: series.intervalMonths ?? 1,
    count: series.count ?? null,
    amount: series.amount ?? null,
    subject: series.subject ?? "",
    comment: series.comment ?? null,
    autoSetPaid: series.autoSetPaid ?? false,
    created: series.created ?? null,
    // Defaults to a whole-series edit; SeriesEffectiveFrom sets a date when the user picks "valid from".
    effectiveFrom: null,
    // No scope chosen yet; SeriesEffectiveFrom sets it. A started series requires a choice (see schema),
    // a future-only series treats null as "WHOLE".
    changeScope: null,
  };
}

/** Blank form for a series that doesn't exist yet — the empty DO run through the same normalisation. */
export function emptyLiquiditySeriesValues(): LiquiditySeriesValues {
  return toSeriesFormValues({ id: null });
}
