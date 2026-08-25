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
import type {
  CreditorInvoicePositionValues,
  CreditorInvoiceValues,
} from "./creditor-invoice-schema";
import type {
  CreditorInvoiceDetail,
  CreditorInvoicePositionDto,
} from "./types";

// The cost-split arithmetic and the fresh-cost-assignment helper are identical on both invoices and live
// in components/shared/invoice; re-exported here so this feature's call sites and the value tests import
// them from one place.
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
export function toFormValues(
  invoice: CreditorInvoiceDetail
): CreditorInvoiceValues {
  return {
    id: invoice.id ?? null,
    datum: invoice.datum ?? null,
    betreff: invoice.betreff ?? null,
    kreditor: invoice.kreditor ?? null,
    konto: invoice.konto ?? null,
    referenz: invoice.referenz ?? null,
    customernr: invoice.customernr ?? null,
    receiver: invoice.receiver ?? null,
    iban: invoice.iban ?? null,
    bic: invoice.bic ?? null,
    paymentType: invoice.paymentType ?? null,
    faelligkeit: invoice.faelligkeit ?? null,
    // Both day counts are derived from the dates when the invoice doesn't state them, because they are
    // `@Transient` on `AbstractRechnungDO` and only `recalculate()` fills them — which the read path never
    // calls, so a saved invoice always arrives without them. Without this an opened invoice showed no
    // payment target at all.
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
    // Deleted rows are kept — see the comment on `creditorInvoicePositionSchema`.
    positionen: (invoice.positionen ?? []).map(toPositionValues),
    created: invoice.created ?? null,
  };
}

function toPositionValues(
  pos: CreditorInvoicePositionDto
): CreditorInvoicePositionValues {
  return {
    id: pos.id ?? null,
    deleted: pos.deleted === true,
    number: pos.number ?? null,
    text: pos.text ?? null,
    menge: pos.menge ?? null,
    einzelNetto: pos.einzelNetto ?? null,
    vat: pos.vat ?? null,
    kostZuweisungen: (pos.kostZuweisungen ?? []).map(toKostZuweisungValues),
  };
}

/**
 * Blank form for an invoice that doesn't exist yet — the empty DTO run through the very same
 * normalisation, rather than a second list of the same fields.
 *
 * Nothing is proposed here, the date included: the backend presets it in `newBaseDTO` (date = today), and
 * the edit page fetches `/rs/incomingInvoice/newEntry` for a new entry — so those are the values a user
 * actually sees. This is only the shape the form starts out with.
 */
export function emptyCreditorInvoiceValues(): CreditorInvoiceValues {
  return toFormValues({ id: null });
}

/**
 * A fresh position, carrying the number it will be stored with.
 *
 * Numbered here rather than only on save because the header of a row shows its number, and a preview that
 * differs from what the backend then assigns is worse than none. The backend still has the last word.
 *
 * @param number What [nextPositionNumber] yields for the rows the form currently holds.
 * @param predecessor The row it is added below, if any. Its VAT rate is proposed — every position of an
 *   invoice is taxed the same in all but the rare case.
 * @param defaultVat The configured `fibu.defaultVAT`, used where there is no predecessor to take a rate
 *   from — the row above is a better guess about *this* invoice than a value configured installation-wide.
 */
export function emptyPositionValues(
  number: number,
  predecessor?: CreditorInvoicePositionValues,
  defaultVat?: number | null
): CreditorInvoicePositionValues {
  return toPositionValues({
    number,
    vat: predecessor?.vat ?? defaultVat ?? null,
  });
}
