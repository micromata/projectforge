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
 *
 * @param overdue Decides overdue instead of the dates, where the server answered it: the creditor invoice
 *   reads overdue already 2 working days before its due date (`RechnungInfo.isMarkedOverdue`, holidays
 *   aware). Undefined/null while that answer is on its way, then the dates decide.
 */
export function paidState(
  bezahlDatum: string | null | undefined,
  faelligkeit: string | null | undefined,
  overdue?: boolean | null
): PaidState {
  if (bezahlDatum)
    return { tone: "success", labelKey: "fibu.rechnung.status.bezahlt" };
  // LocalDate strings ("yyyy-MM-dd") compare lexicographically, so `< today` is "before today".
  if (overdue ?? (!!faelligkeit && faelligkeit < todayIso()))
    return { tone: "danger", labelKey: "fibu.rechnung.filter.ueberfaellig" };
  return { tone: "info", labelKey: "fibu.rechnung.filter.unbezahlt" };
}
