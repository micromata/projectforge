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
  /** The series this entry belongs to, or null for a plain entry. Materialized occurrences carry it. */
  seriesId?: number | null;
  /** The occurrence's stable anchor day within its series — its identity, not the editable dateOfPayment. */
  seriesDate?: string | null;
  /** The "repeat" block that turns a new entry into a recurring series (never set on a loaded entry). */
  repeat?: LiquidityRepeat | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Mirrors org.projectforge.plugins.liquidityplanning.LiquidityRepeatConfig — the new-entry "repeat" block. */
export interface LiquidityRepeat {
  enabled: boolean;
  intervalMonths: number;
  /** null = endless. */
  count: number | null;
}

/** The recurring series as its focused editor reads and writes it — `LiquiditySeriesDO` as-is. */
export interface LiquiditySeriesDetail {
  /** null for a series that has not been saved yet (there is no add page — a series is born via `repeat`). */
  id: number | null;
  /** Anchor of the recurrence: the first occurrence's date and the day-of-month every occurrence keeps. */
  startDate?: string | null;
  /** The recurrence unit — a `RecurrenceFrequency` constant; only `MONTHLY` is offered for now. */
  frequency?: string | null;
  /** Every how many months an occurrence falls (1 = every month). */
  intervalMonths?: number | null;
  /** Number of installments, or null for an endless series. */
  count?: number | null;
  amount?: number | null;
  subject?: string | null;
  comment?: string | null;
  autoSetPaid?: boolean | null;
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
  /** Set on a row that belongs to a series — a materialized occurrence, or a projected virtual one. */
  seriesId?: number | null;
  /** The occurrence's stable anchor day within its series; carried so a click can materialize it. */
  seriesDate?: string | null;
  created?: string;
  lastUpdate?: string;
}

/**
 * A projected, not-yet-stored series occurrence: the backend gives it a negative synthetic id
 * (`LiquiditySeriesProjector`), while every real row's PK is positive. That sign is the only "virtual"
 * flag — a click on such a row materializes it rather than opening a stored entry (see LIQUIDITY_PAGE).
 */
export function isVirtualRow(row: LiquidityListRow): boolean {
  return row.id != null && Number(row.id) < 0;
}
