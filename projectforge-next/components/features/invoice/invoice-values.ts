import type {
  InvoicePositionValues,
  InvoiceValues,
  KostZuweisungValues,
} from "./invoice-schema";
import type {
  InvoiceDetail,
  InvoicePositionDto,
  KostZuweisungDto,
} from "./types";

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
    zahlungsZielInTagen: invoice.zahlungsZielInTagen ?? null,
    discountZahlungsZielInTagen: invoice.discountZahlungsZielInTagen ?? null,
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

function toKostZuweisungValues(
  assignment: KostZuweisungDto
): KostZuweisungValues {
  return {
    id: assignment.id ?? null,
    deleted: assignment.deleted === true,
    index: assignment.index ?? null,
    netto: assignment.netto ?? null,
    kost1: assignment.kost1 ?? null,
    kost2: assignment.kost2 ?? null,
    comment: assignment.comment ?? null,
  };
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
 *   invoice is taxed the same in all but the rare case, and `AbstractRechnungEditForm` presets it the
 *   same way. Nothing else is: what is billed is what the new row is there to say.
 */
export function emptyPositionValues(
  number: number,
  predecessor?: InvoicePositionValues
): InvoicePositionValues {
  return toPositionValues({ number, vat: predecessor?.vat ?? null });
}

/**
 * The number the next position gets: one past the highest in the form, deleted and stored rows
 * included — the number is what `UNIQUE(rechnung_fk, number)` and the collection handler identify a
 * position by, so reusing one would collide with a row that is still in the database.
 */
export function nextPositionNumber(
  positions: readonly InvoicePositionValues[]
): number {
  return positions.reduce((max, pos) => Math.max(max, pos.number ?? 0), 0) + 1;
}

/**
 * A fresh cost assignment.
 *
 * @param index What [nextKostZuweisungIndex] yields for the rows the position currently holds.
 * @param predecessor The row it is added below, if any. Its two cost units are proposed — splitting a
 *   position across cost 2 units usually keeps cost 1, and Wicket's dialog carries them over the same
 *   way. The amount is not: what is left over is the whole point of the Fehlbetrag.
 */
export function emptyKostZuweisungValues(
  index: number,
  predecessor?: KostZuweisungValues
): KostZuweisungValues {
  return toKostZuweisungValues({
    index,
    kost1: predecessor?.kost1 ?? null,
    kost2: predecessor?.kost2 ?? null,
  });
}

/**
 * The index the next cost assignment of a position gets: one past the highest that position holds,
 * deleted and stored rows included — `KostZuweisungDO`'s identity is `(index, owner)`, so reusing one
 * would merge a new row with a deleted row's past.
 *
 * **0-based**, unlike a position's number: an empty list starts at 0, which is what
 * `KostZuweisungDO.addKostZuweisung` does.
 */
export function nextKostZuweisungIndex(
  assignments: readonly KostZuweisungValues[]
): number {
  return assignments.length === 0
    ? 0
    : assignments.reduce((max, entry) => Math.max(max, entry.index ?? 0), 0) +
        1;
}
