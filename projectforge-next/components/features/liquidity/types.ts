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
  paid?: boolean | null;
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
  subject?: string;
  comment?: string;
  created?: string;
  lastUpdate?: string;
}
