import { z } from "zod";
import { KOST_ZUWEISUNG_METADATA } from "@/lib/metadata/kost-zuweisung.generated";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the constants of each enum — comes from `RechnungDO`,
 * `RechnungsPositionDO` and `KostZuweisungDO` through `lib/metadata/*.generated.ts`, the same source the
 * field components read (see form-context.tsx). Which fields the form has mirrors the three DTOs in
 * projectforge-rest — that is a hand-written decision, because a DTO has neither the field set nor the
 * names of its DO. What each field *allows* is not.
 *
 * The rules the metadata cannot express are the backend's alone and stay there: everything
 * `RechnungDao.onInsertOrModify`/`validate` checks (a date is required, an invoice needs at least one
 * position, a number may not be reused) and the period of performance. They come back as an HTTP 406 and
 * land on the field their `fieldId` names, nested paths included
 * (`positionen[0].kostZuweisungen[1].netto`).
 *
 * Deliberately **not** validated here: the sum of a position's cost assignments against its net sum.
 * `RechnungDao` performs no such check either, so an invoice with a difference has always been savable —
 * the form shows the Fehlbetrag in red instead.
 */
const m = fromMetadata(RECHNUNG_METADATA);
const p = fromMetadata(RECHNUNGS_POSITION_METADATA);
const k = fromMetadata(KOST_ZUWEISUNG_METADATA);

/**
 * A referenced entity none of the three metadata objects has a field for: the customer, the project and
 * the account are `KundeDO`, `ProjektDO` and `KontoDO`, for which there is no `UIDataType`, so
 * `ElementsRegistry` never reports them and the generator cannot carry them (see UIDataTypeUtils).
 * Written by id like every other reference — and under the DTO's names `customer`/`project`, not the
 * entity's `kunde`/`projekt`.
 */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One cost assignment of a position — the third nesting level of this form.
 *
 * `index` travels back untouched, and a removed row stays with `deleted = true`, for the same reason a
 * position does: `RechnungsPositionDO.kostZuweisungen` carries `autoUpdateCollectionEntries` but no
 * `@SoftDeleteCollection`, and `KostZuweisungDO.equals` matches on `(index, owner)` — so an omitted or
 * renumbered row reads as "removed" to the collection handler and is deleted physically, history and all.
 */
export const kostZuweisungSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  /** 0-based, unlike a position's 1-based number (`KostZuweisungDO.addKostZuweisung`). */
  index: z.number().nullable(),
  netto: k.decimalField("netto"),
  kost1: entityRef,
  kost2: entityRef,
  comment: k.nullableString("comment"),
});

/**
 * One invoice position. Kept in the values even when deleted, with `deleted = true`: the backend's
 * `CollectionHandler` physically removes — history and all — whatever a posted collection leaves out
 * (`RechnungDO.positionen` has `autoUpdateCollectionEntries` but no `@SoftDeleteCollection`; only
 * `EingangsrechnungDO.positionen` has that).
 *
 * `number` travels back untouched: it is `@OrderColumn` with `@ListIndexBase(1)` and part of
 * `UNIQUE(rechnung_fk, number)`, so renumbering an existing position would collide or read as
 * "removed and added".
 */
export const invoicePositionSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  number: z.number().nullable(),
  text: p.nullableString("text"),
  menge: p.decimalField("menge"),
  einzelNetto: p.decimalField("einzelNetto"),
  /** The VAT rate as a factor: 0.19 for 19 %, as `RechnungsPositionDO.vat` holds it. */
  vat: p.decimalField("vat"),
  periodOfPerformanceType: p.enumField("periodOfPerformanceType"),
  periodOfPerformanceBegin: p.nullableString("periodOfPerformanceBegin"),
  periodOfPerformanceEnd: p.nullableString("periodOfPerformanceEnd"),
  /**
   * The order position this one bills, picked through `order/positionAutosearch` (see
   * OrderPositionField). Only `id` travels back — `RechnungsPosition.copyTo` writes nothing else — but
   * the whole reference is held, because the row header links to the order by `auftragId` and names it
   * by `auftragNummer`.`number`, and both are gone from the values once the user picks a new position.
   *
   * All fields nullable: a picked hit carries them, a value from an older stored invoice may not.
   */
  auftragsPosition: z
    .looseObject({
      id: z.number().nullable().optional(),
      auftragId: z.number().nullable().optional(),
      auftragNummer: z.number().nullable().optional(),
      number: z.number().nullable().optional(),
      displayName: z.string().nullable().optional(),
    })
    .nullable(),
  kostZuweisungen: z.array(kostZuweisungSchema),
});

export const invoiceSchema = z.object({
  // null while the invoice is new — Spring assigns the id.
  id: z.number().nullable(),
  /**
   * Read-only, and the one field deliberately taken away from validation: `RechnungDao` assigns it on the
   * transition out of GEPLANT, and a `GUTSCHRIFTSANZEIGE_DURCH_KUNDEN` must have none at all. Requiring
   * it here would refuse to save any planned invoice.
   */
  nummer: z.number().nullable(),
  datum: m.nullableString("datum"),
  status: m.enumField("status"),
  typ: m.enumField("typ"),
  betreff: m.nullableString("betreff"),
  customer: entityRef,
  kundeText: m.nullableString("kundeText"),
  project: entityRef,
  konto: entityRef,
  customerref1: m.nullableString("customerref1"),
  attachment: m.nullableString("attachment"),
  // The address block of the e-invoice. Prefilled from the customer's account where empty, then the
  // user's — the export itself (XRechnung/ZUGFeRD) is not part of this page yet.
  customerContactPerson: m.nullableString("customerContactPerson"),
  customerAddress: m.nullableString("customerAddress"),
  customerZipCode: m.nullableString("customerZipCode"),
  customerCity: m.nullableString("customerCity"),
  customerCountry: m.nullableString("customerCountry"),
  customerVatId: m.nullableString("customerVatId"),
  customerLeitwegId: m.nullableString("customerLeitwegId"),
  customerEInvoiceEmail: m.nullableString("customerEInvoiceEmail"),
  /**
   * The IBAN of one of the seller's bank accounts, chosen from `EInvoiceSellerConfig.bankAccounts` (see
   * SellerBankAccountField). A string and not an id: the accounts come from the application configuration
   * and have none, and the IBAN is both what the column holds and what `findBankAccount` resolves.
   */
  sellerBankAccount: m.nullableString("sellerBankAccount"),
  periodOfPerformanceBegin: m.nullableString("periodOfPerformanceBegin"),
  periodOfPerformanceEnd: m.nullableString("periodOfPerformanceEnd"),
  faelligkeit: m.nullableString("faelligkeit"),
  /**
   * Days from `datum` to the due date. A transient property of the entity, not a column: the backend
   * derives `faelligkeit` from it (`AuftragAndRechnungDaoHelper.onSaveOrModify`), which is why the form
   * offers it only while `faelligkeit` is empty — as `AbstractRechnungEditForm` does.
   */
  zahlungsZielInTagen: m.intField("zahlungsZielInTagen", { min: 0 }),
  discountZahlungsZielInTagen: m.intField("discountZahlungsZielInTagen", {
    min: 0,
  }),
  discountPercent: m.decimalField("discountPercent"),
  discountMaturity: m.nullableString("discountMaturity"),
  bezahlDatum: m.nullableString("bezahlDatum"),
  zahlBetrag: m.decimalField("zahlBetrag"),
  /**
   * Held but never shown: the currency of an invoice is the installation's, and no frontend ever offered
   * a box for it. Still part of the values — a key Spring doesn't receive leaves the DTO's field null,
   * which `Rechnung.copyTo` would then write over the stored value.
   */
  currency: m.nullableString("currency"),
  bemerkung: m.nullableString("bemerkung"),
  besonderheiten: m.nullableString("besonderheiten"),
  positionen: z.array(invoicePositionSchema),
  created: m.nullableString("created"),
});

export type InvoiceValues = z.infer<typeof invoiceSchema>;
export type InvoicePositionValues = z.infer<typeof invoicePositionSchema>;
export type KostZuweisungValues = z.infer<typeof kostZuweisungSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees. The nested paths of
 * the two collections are matched by their array's name, which is in here.
 */
export const INVOICE_FIELDS = Object.keys(
  invoiceSchema.shape
) as readonly (keyof InvoiceValues)[];

/**
 * Names of the array fields of this form. A bare server error on one of these — most of all
 * `fibu.rechnung.error.rechnungHatKeinePositionen` — has no mounted `<form.Field>` to display it and
 * must surface as a toast instead of being silently dropped into a field slot that nobody reads.
 *
 * `kostZuweisungen` is not in here: it is a field *of a position*, so an error on it carries the full
 * path and lands on the row that has it.
 */
export const INVOICE_ARRAY_FIELDS: readonly string[] = ["positionen"];
