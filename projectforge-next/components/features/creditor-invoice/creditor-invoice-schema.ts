import { z } from "zod";
import { EINGANGSRECHNUNG_METADATA } from "@/lib/metadata/eingangsrechnung.generated";
import { EINGANGSRECHNUNGS_POSITION_METADATA } from "@/lib/metadata/eingangsrechnungs-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import {
  kostZuweisungSchema,
  type KostZuweisungValues,
} from "@/components/shared/invoice/kost-zuweisung";

/**
 * Every rule below — mandatory, maximum length, the constants of `paymentType` — comes from
 * `EingangsrechnungDO` and `EingangsrechnungsPositionDO` through `lib/metadata/*.generated.ts`, the same
 * source the field components read (see form-context.tsx). Which fields the form has mirrors the two DTOs
 * in projectforge-rest — that is a hand-written decision, because a DTO has neither the field set nor the
 * names of its DO. What each field *allows* is not.
 *
 * The rules the metadata cannot express are the backend's alone and stay there: everything
 * `EingangsrechnungDao.onInsertOrModify`/`validate` checks (a date is required, an invoice needs at least
 * one position). They come back as an HTTP 406 and land on the field their `fieldId` names, nested paths
 * included (`positionen[0].kostZuweisungen[1].netto`).
 *
 * Deliberately **not** validated here: the sum of a position's cost assignments against its net sum.
 * `EingangsrechnungDao` performs no such check either, so an invoice with a difference has always been
 * savable — the form shows the Fehlbetrag in red instead.
 *
 * The cost assignment is identical to the outgoing invoice's and is reused from the shared module
 * ([kostZuweisungSchema]); the incoming invoice differs in its head (a creditor and a reference instead of
 * a customer, a project and a status) and in a leaner position (no order link, no period of performance).
 */
const m = fromMetadata(EINGANGSRECHNUNG_METADATA);
const p = fromMetadata(EINGANGSRECHNUNGS_POSITION_METADATA);

/**
 * The DATEV account of the invoice — `KontoDO`, for which there is no `UIDataType`, so `ElementsRegistry`
 * never reports it and the generated metadata cannot carry it. Written by id like every other reference
 * (see AccountField), and held with the name it shows.
 */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One invoice position. Kept in the values even when deleted, with `deleted = true`: `number` travels
 * back untouched, for the reason a cost assignment's `index` does — the collection is matched by it.
 *
 * Leaner than the outgoing position: an incoming invoice bills no order and states no period of
 * performance of its own, so neither `auftragsPosition` nor the period fields are here.
 */
export const creditorInvoicePositionSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  number: z.number().nullable(),
  text: p.nullableString("text"),
  menge: p.decimalField("menge"),
  einzelNetto: p.decimalField("einzelNetto"),
  /** The VAT rate as a factor: 0.19 for 19 %, as `EingangsrechnungsPositionDO.vat` holds it. */
  vat: p.decimalField("vat"),
  kostZuweisungen: z.array(kostZuweisungSchema),
});

export const creditorInvoiceSchema = z.object({
  // null while the invoice is new — Spring assigns the id.
  id: z.number().nullable(),
  datum: m.nullableString("datum"),
  betreff: m.nullableString("betreff"),
  /** The creditor the invoice is from — free text, unlike the outgoing invoice's `customer` reference. */
  kreditor: m.nullableString("kreditor"),
  konto: entityRef,
  referenz: m.nullableString("referenz"),
  customernr: m.nullableString("customernr"),
  receiver: m.nullableString("receiver"),
  iban: m.nullableString("iban"),
  bic: m.nullableString("bic"),
  paymentType: m.enumField("paymentType"),
  faelligkeit: m.nullableString("faelligkeit"),
  /**
   * Days from `datum` to the due date and to the discount date. Transient properties of the entity, not
   * columns: the backend derives the dates from them while those are empty
   * (`AuftragAndRechnungDaoHelper.onSaveOrModify`), which is why the form only lets them be typed for a new
   * invoice and shows what the dates say afterwards (see PaymentFields).
   *
   * Deliberately **unbounded** for the same reason as the outgoing invoice: a stored invoice reads these
   * off the dates, and a discount date before the invoice date is in the data.
   */
  zahlungsZielInTagen: m.intField("zahlungsZielInTagen"),
  discountZahlungsZielInTagen: m.intField("discountZahlungsZielInTagen"),
  discountPercent: m.decimalField("discountPercent"),
  discountMaturity: m.nullableString("discountMaturity"),
  bezahlDatum: m.nullableString("bezahlDatum"),
  zahlBetrag: m.decimalField("zahlBetrag"),
  /**
   * Held but never shown: the currency of an invoice is the installation's, and no frontend ever offered a
   * box for it. Still part of the values — a key Spring doesn't receive leaves the DTO's field null, which
   * `Eingangsrechnung.copyTo` would then write over the stored value.
   */
  currency: m.nullableString("currency"),
  bemerkung: m.nullableString("bemerkung"),
  besonderheiten: m.nullableString("besonderheiten"),
  positionen: z.array(creditorInvoicePositionSchema),
  created: m.nullableString("created"),
});

export type CreditorInvoiceValues = z.infer<typeof creditorInvoiceSchema>;
export type CreditorInvoicePositionValues = z.infer<
  typeof creditorInvoicePositionSchema
>;
// Re-exported from the shared module so this feature's imports resolve here — the cost assignment is
// identical on both invoices and lives in components/shared/invoice/kost-zuweisung.ts.
export type { KostZuweisungValues };

/**
 * Field names of the form, so a server validation error can be checked against what actually renders (see
 * applyServerValidationErrors) instead of vanishing into a field nobody sees. The nested paths of the
 * positions collection are matched by its array's name, which is in here.
 */
export const CREDITOR_INVOICE_FIELDS = Object.keys(
  creditorInvoiceSchema.shape
) as readonly (keyof CreditorInvoiceValues)[];

/**
 * Names of the array fields of this form. A bare server error on one of these — most of all
 * `fibu.rechnung.error.rechnungHatKeinePositionen` — has no mounted `<form.Field>` to display it and must
 * surface as a toast instead of being dropped into a field slot nobody reads.
 *
 * `kostZuweisungen` is not in here: it is a field *of a position*, so an error on it carries the full path
 * and lands on the row that has it.
 */
export const CREDITOR_INVOICE_ARRAY_FIELDS: readonly string[] = ["positionen"];
