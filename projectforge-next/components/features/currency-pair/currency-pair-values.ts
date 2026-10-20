import type { CurrencyPairValues } from "./currency-pair-schema";
import type { CurrencyPairDetail } from "./types";

export function emptyCurrencyPairValues(): CurrencyPairValues {
  return {
    id: null,
    sourceCurrency: "",
    targetCurrency: "",
    comment: null,
  };
}

export function toFormValues(pair: CurrencyPairDetail): CurrencyPairValues {
  return {
    id: pair.id ?? null,
    sourceCurrency: pair.sourceCurrency ?? "",
    targetCurrency: pair.targetCurrency ?? "",
    comment: pair.comment ?? null,
  };
}
