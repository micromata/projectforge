// Mirrors org.projectforge.rest.dto.CurrencyPair and CurrencyConversionRate (projectforge-rest). Keep field names
// in sync with the Spring DTOs — currentRate, currentInverseRate and currentRateDate are read-only, the rate in
// force today; rateEntries come with the form only.

import type { ValidityEntry } from "@/components/shared/validity-entries/types";

/** One conversion rate of a pair, valid from its date on until the next one starts. */
export interface CurrencyConversionRate extends ValidityEntry {
  validFrom?: string | null;
  /** 1 source currency = conversionRate target currency. */
  conversionRate?: number | null;
  /** 1 target currency = inverseConversionRate source currency. */
  inverseConversionRate?: number | null;
  currencyPairId?: number | null;
}

export interface CurrencyPairDetail {
  /** null for a pair that has not been saved yet (Spring assigns the id). */
  id: number | null;
  displayName?: string | null;
  sourceCurrency?: string | null;
  targetCurrency?: string | null;
  comment?: string | null;
  /** Read-only, in force today. */
  currentRate?: number | null;
  /** Read-only, in force today. */
  currentInverseRate?: number | null;
  /** Read-only: the date the rate in force today is valid from. */
  currentRateDate?: string | null;
  /** Only in the form (`transformFromDB` in edit mode), newest first. */
  rateEntries?: CurrencyConversionRate[] | null;
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
}

export interface CurrencyPairListRow extends CurrencyPairDetail {
  id: number;
}
