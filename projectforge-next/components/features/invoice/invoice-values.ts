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
 *   way.
 * @param netto What of the position is still unassigned, which is what the new row is most likely for:
 *   the whole net sum on the first row, the rest on a later one. A proposal like the two cost units and
 *   nothing more — the field stays editable, and the Fehlbetrag still says whether it adds up.
 *   `RechnungCostEditTablePanel.addZuweisung` presets the same amount.
 */
export function emptyKostZuweisungValues(
  index: number,
  predecessor?: KostZuweisungValues,
  netto?: number | null
): KostZuweisungValues {
  return toKostZuweisungValues({
    index,
    netto,
    kost1: predecessor?.kost1 ?? null,
    kost2: predecessor?.kost2 ?? null,
  });
}

/**
 * What of a position's net sum is not assigned to a cost unit yet — the amount a new row is proposed
 * with ([emptyKostZuweisungValues]), and `null` where there is nothing to propose.
 *
 * Half server, half form on purpose. `netSum` is the server's (`RechnungCalculator` rounds a position
 * before it enters a sum, which is German law and not to be reimplemented here — see [useInvoiceSums]),
 * while what is already assigned is read from the form: the sums are debounced by 400 ms, so a row added
 * right after an amount was typed would otherwise be prefilled from a number that is no longer on
 * screen. No rounding rule is duplicated by this — it only adds up amounts the user entered.
 *
 * Deleted rows don't count, as they don't for `RechnungCalculator`. A remaining of zero yields `null`
 * rather than 0: an empty box is filled in, a `0,00 €` has to be cleared first. An over-assigned
 * position yields a negative amount, which is what Wicket proposes too — the row that is one too many
 * should read as one too many.
 */
export function remainingNet(
  netSum: number | null | undefined,
  assignments: readonly KostZuweisungValues[]
): number | null {
  if (netSum == null) return null;
  const assigned = assignments.reduce(
    (sum, entry) => (entry.deleted ? sum : sum + (entry.netto ?? 0)),
    0
  );
  // Rounded to the two digits an amount has: 2000 - 1900.1 is 99.90000000000009 in binary floating
  // point, and that is what would land in the box.
  const remaining = Number((netSum - assigned).toFixed(2));
  return remaining === 0 ? null : remaining;
}

/**
 * The share of its position one cost assignment carries, as a fraction — 0.5 for half of it.
 *
 * `null` where there is no share to state: an amount that is not filled in yet, or a position that sums
 * to nothing, which is also what keeps the division defined. Wicket's `Prozent` column leaves its cell
 * blank in exactly those two cases (`RechnungCostEditTablePanel`).
 *
 * Unrounded: what it is rounded to is the reader's business, not the ratio's (see
 * [formatPercentageDecimal], which the two places showing this pass 0 digits to).
 */
export function shareOfNetSum(
  netto: number | null | undefined,
  netSum: number | null | undefined
): number | null {
  if (netto == null || netto === 0 || netSum == null || netSum === 0)
    return null;
  return netto / netSum;
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
