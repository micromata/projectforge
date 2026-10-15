import { todayIso } from "@/lib/date-parse";
import type { StatusTone } from "@/components/shared/status-pill";

/** The paid state derived from an invoice's dates, with the traffic-light tone and label key it reads as. */
export type PaidState = { tone: StatusTone; labelKey: string };

/**
 * Whether an invoice is paid, overdue or still unpaid — derived from its dates because neither the creditor
 * invoice (no status enum) nor an outgoing invoice still in `GESTELLT` says it any other way, and the list's
 * `ueberfaellig` flag never reaches the edit form. A payment date means paid; else a **due date** in the past
 * means overdue; else it is simply unpaid.
 *
 * Overdue keys off `faelligkeit` alone, not the discount (Skonto) maturity: missing the discount deadline
 * costs the discount, it does not make the invoice overdue — that is what `RechnungInfo.isUeberfaellig` reads
 * too. Using the earlier of the two dates would wrongly flag an unpaid-but-not-yet-due invoice as overdue.
 */
export function paidState(
  bezahlDatum: string | null | undefined,
  faelligkeit: string | null | undefined
): PaidState {
  if (bezahlDatum)
    return { tone: "success", labelKey: "fibu.rechnung.status.bezahlt" };
  // LocalDate strings ("yyyy-MM-dd") compare lexicographically, so `< today` is "before today".
  if (faelligkeit && faelligkeit < todayIso())
    return { tone: "danger", labelKey: "fibu.rechnung.filter.ueberfaellig" };
  return { tone: "info", labelKey: "fibu.rechnung.filter.unbezahlt" };
}
