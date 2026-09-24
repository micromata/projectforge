// Mirrors org.projectforge.plugins.liquidityplanning.LiquidityEntryDO — the plain five-field entity the
// hand-built LiquidityEntityRest transfers as-is (no DTO in between). The list row and the edit detail are
// the same shape here; the DO carries nothing the list omits.
//
// Every property is optional: Spring's mapper uses `JsonInclude.Include.NON_NULL` (JacksonConfiguration),
// so an empty field is absent from the JSON rather than null.

import type { ListRow } from "@/hooks/use-entity-list-page";

/** The whole liquidity entry as the edit form reads and writes it — `LiquidityEntryDO` as-is. */
export interface LiquidityDetail {
  /** null for an entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** The expected date of payment; drives the forecast and the red/blue row highlight. */
  dateOfPayment?: string | null;
  /** Signed amount: negative is a credit (money coming in), positive a debit (money going out). */
  amount?: number | null;
  /** Three-state override: null follows the autoSetPaid rule, true/false force the paid status. */
  paid?: boolean | null;
  /** When set, the entry counts as paid once its date of payment has passed, unless `paid` overrides it. */
  autoSetPaid?: boolean | null;
  /** Derived by the backend: `paid ?? (autoSetPaid && dateOfPayment < today)`. Read-only. */
  effectivePaid?: boolean;
  subject?: string | null;
  comment?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** One row of the liquidity entry list. */
export interface LiquidityListRow extends ListRow {
  dateOfPayment?: string;
  amount?: number;
  paid?: boolean;
  autoSetPaid?: boolean;
  /** Derived paid status the list highlights and shows, sent alongside `paid` by the backend. */
  effectivePaid?: boolean;
  subject?: string;
  comment?: string;
  created?: string;
  lastUpdate?: string;
}
