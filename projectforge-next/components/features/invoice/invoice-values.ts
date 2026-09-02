import { daysBetweenDates } from "@/lib/date-parse";
import {
  emptyKostZuweisungValues,
  toKostZuweisungValues,
} from "@/components/shared/invoice/kost-zuweisung";
import {
  nextKostZuweisungIndex,
  nextPositionNumber,
  remainingNet,
  shareOfNetSum,
} from "@/components/shared/invoice/values";
import type { InvoicePositionValues, InvoiceValues } from "./invoice-schema";
import type { InvoiceDetail, InvoicePositionDto, OrderRef } from "./types";

// The cost-split arithmetic and the fresh-cost-assignment helper are identical on both invoices and now
// live in components/shared/invoice; re-exported here so this feature's existing call sites and the
// value tests keep importing them from one place.
export {
  emptyKostZuweisungValues,
  nextKostZuweisungIndex,
  nextPositionNumber,
  remainingNet,
  shareOfNetSum,
};

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 *
 * Module level and never wrapped in a hook: `useEntityEditForm` resets the form whenever this function
 * changes identity, so a per-render one would reset on every render and throw away what is being typed.
 */
export function toFormValues(invoice: InvoiceDetail): InvoiceValues {
  return {
    id: invoice.id ?? null,
    nummer: invoice.nummer ?? null,
    datum: invoice.datum ?? null,
    status: invoice.status ?? null,
    typ: invoice.typ ?? null,
    betreff: invoice.betreff ?? null,
    customer: invoice.customer ?? null,
    kundeText: invoice.kundeText ?? null,
    project: invoice.project ?? null,
    konto: invoice.konto ?? null,
    customerref1: invoice.customerref1 ?? null,
    attachment: invoice.attachment ?? null,
    customerContactPerson: invoice.customerContactPerson ?? null,
    customerAddress: invoice.customerAddress ?? null,
    customerZipCode: invoice.customerZipCode ?? null,
    customerCity: invoice.customerCity ?? null,
    customerCountry: invoice.customerCountry ?? null,
    customerVatId: invoice.customerVatId ?? null,
    customerLeitwegId: invoice.customerLeitwegId ?? null,
    customerEInvoiceEmail: invoice.customerEInvoiceEmail ?? null,
    sellerBankAccount: invoice.sellerBankAccount ?? null,
    periodOfPerformanceBegin: invoice.periodOfPerformanceBegin ?? null,
    periodOfPerformanceEnd: invoice.periodOfPerformanceEnd ?? null,
    faelligkeit: invoice.faelligkeit ?? null,
    // Both day counts are derived from the dates when the invoice doesn't state them, because they are
    // `@Transient` on `AbstractRechnungDO` and only `recalculate()` fills them — which the read path
    // never calls, so a saved invoice always arrives without them. Without this an opened invoice showed
    // no payment target at all, and cloning it (`prepareInvoiceClone` computes `today + days`) made the
    // copy due on the day it was created.
    zahlungsZielInTagen:
      invoice.zahlungsZielInTagen ??
      daysBetweenDates(invoice.datum, invoice.faelligkeit),
    discountZahlungsZielInTagen:
      invoice.discountZahlungsZielInTagen ??
      daysBetweenDates(invoice.datum, invoice.discountMaturity),
    discountPercent: invoice.discountPercent ?? null,
    discountMaturity: invoice.discountMaturity ?? null,
    bezahlDatum: invoice.bezahlDatum ?? null,
    zahlBetrag: invoice.zahlBetrag ?? null,
    currency: invoice.currency ?? null,
    bemerkung: invoice.bemerkung ?? null,
    besonderheiten: invoice.besonderheiten ?? null,
    // Deleted rows are kept — see the comment on `invoicePositionSchema`.
    positionen: (invoice.positionen ?? []).map(toPositionValues),
    created: invoice.created ?? null,
  };
}

function toPositionValues(pos: InvoicePositionDto): InvoicePositionValues {
  return {
    id: pos.id ?? null,
    deleted: pos.deleted === true,
    number: pos.number ?? null,
    text: pos.text ?? null,
    menge: pos.menge ?? null,
    einzelNetto: pos.einzelNetto ?? null,
    vat: pos.vat ?? null,
    // SEEABOVE is the entity's own default (`RechnungsPositionDO`): a position follows the invoice's
    // period unless it is given one.
    periodOfPerformanceType: pos.periodOfPerformanceType ?? "SEEABOVE",
    periodOfPerformanceBegin: pos.periodOfPerformanceBegin ?? null,
    periodOfPerformanceEnd: pos.periodOfPerformanceEnd ?? null,
    // Handed back untouched, id included: `Rechnung.copyTo` resolves the order position by it, and
    // dropping the key would unlink a position that was billed against an order.
    auftragsPosition: pos.auftragsPosition ?? null,
    kostZuweisungen: (pos.kostZuweisungen ?? []).map(toKostZuweisungValues),
  };
}

/**
 * The orders this invoice bills, each one once and by number — derived from the positions rather than
 * carried by the DTO: the edit form holds the reference per position ([InvoicePositionValues.auftragsPosition],
 * with the order's id and number), and this is the same distinct-and-sorted reduction the backend does
 * for the list row (`Rechnung.copyFrom4ListRow`), so the banner can show the "Aufträge" links the list
 * has without a second query or a new field.
 *
 * Deleted positions are skipped (their reference is on its way out), and a position without a resolved
 * order id contributes nothing — a reference the reader may not follow arrives without one.
 */
export function referencedOrders(
  positionen: InvoicePositionValues[] | undefined
): OrderRef[] {
  const byId = new Map<number, OrderRef>();
  for (const pos of positionen ?? []) {
    if (pos.deleted) continue;
    const id = pos.auftragsPosition?.auftragId;
    if (id == null) continue;
    if (!byId.has(id)) {
      byId.set(id, {
        id,
        nummer: pos.auftragsPosition?.auftragNummer ?? undefined,
      });
    }
  }
  return [...byId.values()].sort((a, b) => (a.nummer ?? 0) - (b.nummer ?? 0));
}

/**
 * Blank form for an invoice that doesn't exist yet — the empty DTO run through the very same
 * normalisation, rather than a second list of the same fields: two lists are two places for a field to
 * be forgotten in, and the one that would be forgotten is this one.
 *
 * Nothing is proposed here, date and status included: the backend presets them in `newBaseDTO` (date =
 * today, status = GESTELLT, type = RECHNUNG), and the edit page fetches `/rs/outgoingInvoice/newEntry`
 * for a new entry — so those are the values a user actually sees. This is only the shape the form starts
 * out with.
 */
export function emptyInvoiceValues(): InvoiceValues {
  return toFormValues({ id: null });
}

/**
 * A fresh position, carrying the number it will be stored with.
 *
 * Numbered here rather than only on save because the header of a row shows its number, and a preview
 * that differs from what the backend then assigns is worse than none. The backend still has the last
 * word (`OutgoingInvoiceEntityRest.transformForDB`).
 *
 * @param number What [nextPositionNumber] yields for the rows the form currently holds.
 * @param predecessor The row it is added below, if any. Its VAT rate is proposed — every position of an
 *   invoice is taxed the same in all but the rare case. Nothing else is: what is billed is what the new
 *   row is there to say.
 * @param defaultVat The configured `fibu.defaultVAT`, used where there is no predecessor to take a rate
 *   from. Wicket presets only this one (`AbstractRechnungEditForm.refreshPositions`) and even then only on
 *   the very first position; carrying the predecessor's rate over is an improvement kept on purpose,
 *   which is why it wins here — the row above is a better guess about *this* invoice than a value
 *   configured once for the whole installation.
 */
export function emptyPositionValues(
  number: number,
  predecessor?: InvoicePositionValues,
  defaultVat?: number | null
): InvoicePositionValues {
  return toPositionValues({
    number,
    vat: predecessor?.vat ?? defaultVat ?? null,
  });
}
