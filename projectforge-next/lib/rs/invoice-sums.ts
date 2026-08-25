/**
 * The live sums of an unsaved invoice form, shared by both invoice kinds.
 *
 * The outgoing invoice (`OutgoingInvoiceEntityRest`) and the incoming one
 * (`IncomingInvoiceEntityRest`) answer the very same shape from `POST /rs/{entity}/recalculate`
 * (`InvoiceSums` in either): how a position is rounded before it enters a sum is German law and
 * `RechnungCalculator`'s rule, and neither controller reimplements it. So the request and the two
 * result shapes live here, parameterized by the REST category, rather than once per feature.
 */

import { request } from "./client";
import type { PostData } from "./types";

/** Sums of one position, matched by its number — a new position has no id yet. */
export interface InvoicePositionSums {
  number?: number | null;
  netSum?: number | null;
  vatAmount?: number | null;
  grossSum?: number | null;
  /** Net sum of this position's cost assignments. */
  kostZuweisungNetSum?: number | null;
  /**
   * How much of the position's net sum is not assigned to a cost unit yet — **negated**, as
   * `RechnungPosInfo` computes it: an unassigned rest of 400,00 € reads as -400,00. A hint only, since
   * the backend validates no cost assignment sums.
   */
  kostZuweisungNetFehlbetrag?: number | null;
}

/** What `{Outgoing,Incoming}InvoiceEntityRest.recalculate` answers (`InvoiceSums` there). */
export interface InvoiceSums {
  netSum?: number | null;
  vatAmount?: number | null;
  grossSum?: number | null;
  /** Gross sum minus a discount that was taken — the amount the invoice actually comes to. */
  grossSumWithDiscount?: number | null;
  kostZuweisungenNetSum?: number | null;
  /** The same difference as above for the whole invoice, but **not** negated (`RechnungInfo`). */
  kostZuweisungenFehlbetrag?: number | null;
  bezahlt?: boolean | null;
  ueberfaellig?: boolean | null;
  positions?: InvoicePositionSums[] | null;
}

/**
 * Recalculates every sum of an invoice from the **unsaved** form state, for the given REST category.
 *
 * Needed rather than convenient: how a position is rounded before it enters a sum is German law and
 * `RechnungCalculator`'s rule (`roundPositionsBeforeSum`), and the caches only know saved invoices. So
 * the backend builds a transient `*RechnungDO` from the posted DTO and computes on that, with
 * `useCaches = false` — the posted positions have no ids to look anything up by.
 *
 * Deleted rows may be sent along untouched: the calculator skips them itself.
 *
 * @param entity The REST category, e.g. `outgoingInvoice` or `incomingInvoice`.
 * @param data The form's values, i.e. the same DTO a save would send.
 */
export function recalculateInvoiceSums(
  entity: string,
  data: unknown,
  signal?: AbortSignal
): Promise<InvoiceSums> {
  const postData: PostData = { data } as PostData;
  return request<InvoiceSums>(
    `/rs/${entity}/recalculate`,
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
}
